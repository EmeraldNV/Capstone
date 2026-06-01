package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.config.StripeProperties;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.service.StripeCheckoutService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments/stripe")
public class StripeWebhookController {

    private static final long SIGNATURE_TOLERANCE_SECONDS = 300L;

    private final StripeCheckoutService service;
    private final StripeProperties stripeProperties;
    private final ObjectMapper objectMapper;

    public StripeWebhookController(StripeCheckoutService service, StripeProperties stripeProperties, ObjectMapper objectMapper) {
        this.service = service;
        this.stripeProperties = stripeProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload,
                                              @RequestHeader("Stripe-Signature") String signatureHeader) {
        if (stripeProperties.getWebhookSecret() == null || stripeProperties.getWebhookSecret().isBlank()) {
            throw new BadRequestApiException("STRIPE_WEBHOOK_NOT_CONFIGURED", "Stripe webhook secret is not configured.");
        }

        verifySignature(payload, signatureHeader);

        JsonNode root;
        try {
            root = objectMapper.readTree(payload);
        } catch (Exception ex) {
            throw new BadRequestApiException("STRIPE_EVENT_INVALID", "Stripe webhook payload is invalid.");
        }

        String eventType = root.path("type").asText("");
        JsonNode sessionNode = root.path("data").path("object");
        String sessionId = sessionNode.path("id").asText("");
        String paymentIntentId = sessionNode.path("payment_intent").asText(null);

        switch (eventType) {
            case "checkout.session.completed" -> service.handleCheckoutSessionCompleted(sessionId, paymentIntentId, payload);
            case "checkout.session.expired" -> service.handleCheckoutSessionCanceled(sessionId, payload);
            default -> {
                // Ignore unsupported event types.
            }
        }

        return ResponseEntity.ok().build();
    }

    private void verifySignature(String payload, String signatureHeader) {
        long timestamp = -1L;
        List<String> signatures = new ArrayList<>();

        for (String part : signatureHeader.split(",")) {
            String trimmed = part.trim();
            if (trimmed.startsWith("t=")) {
                timestamp = parseLong(trimmed.substring(2), -1L);
            } else if (trimmed.startsWith("v1=")) {
                signatures.add(trimmed.substring(3));
            }
        }

        if (timestamp <= 0L || signatures.isEmpty()) {
            throw new BadRequestApiException("STRIPE_SIGNATURE_INVALID", "Invalid Stripe webhook signature.");
        }

        long age = Math.abs(Instant.now().getEpochSecond() - timestamp);
        if (age > SIGNATURE_TOLERANCE_SECONDS) {
            throw new BadRequestApiException("STRIPE_SIGNATURE_EXPIRED", "Stripe webhook signature has expired.");
        }

        String signedPayload = timestamp + "." + payload;
        String expectedSignature = hmacSha256Hex(stripeProperties.getWebhookSecret(), signedPayload);
        boolean matches = signatures.stream()
                .anyMatch(signature -> MessageDigest.isEqual(
                        expectedSignature.getBytes(StandardCharsets.UTF_8),
                        signature.getBytes(StandardCharsets.UTF_8)));
        if (!matches) {
            throw new BadRequestApiException("STRIPE_SIGNATURE_INVALID", "Invalid Stripe webhook signature.");
        }
    }

    private long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String hmacSha256Hex(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (Exception ex) {
            throw new BadRequestApiException("STRIPE_SIGNATURE_INVALID", "Invalid Stripe webhook signature.");
        }
    }
}
