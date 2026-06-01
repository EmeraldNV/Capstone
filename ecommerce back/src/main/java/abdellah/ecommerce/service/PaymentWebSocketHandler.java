package abdellah.ecommerce.service;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;

@Component
public class PaymentWebSocketHandler extends TextWebSocketHandler {

    private final PaymentNotificationService paymentNotificationService;
    private final PaymentWebSocketRegistry paymentWebSocketRegistry;

    public PaymentWebSocketHandler(PaymentNotificationService paymentNotificationService,
                                   PaymentWebSocketRegistry paymentWebSocketRegistry) {
        this.paymentNotificationService = paymentNotificationService;
        this.paymentWebSocketRegistry = paymentWebSocketRegistry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = extractSessionId(session.getUri());
        if (sessionId == null || sessionId.isBlank()) {
            session.close(CloseStatus.POLICY_VIOLATION.withReason("Missing sessionId"));
            return;
        }

        session.getAttributes().put("checkoutSessionId", sessionId);
        paymentWebSocketRegistry.register(sessionId, session);
        paymentNotificationService.findUpdate(sessionId)
                .ifPresentOrElse(
                        paymentWebSocketRegistry::broadcast,
                        () -> safeClose(session, CloseStatus.POLICY_VIOLATION.withReason("Stripe session not found"))
                );
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = (String) session.getAttributes().get("checkoutSessionId");
        if (sessionId != null) {
            paymentWebSocketRegistry.unregister(sessionId, session);
        }
    }

    private void safeClose(WebSocketSession session, CloseStatus status) {
        try {
            session.close(status);
        } catch (Exception ignored) {
            // ignore close failures
        }
    }

    private String extractSessionId(URI uri) {
        if (uri == null || uri.getQuery() == null || uri.getQuery().isBlank()) {
            return null;
        }
        for (String pair : uri.getQuery().split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && "sessionId".equals(parts[0])) {
                return decode(parts[1]);
            }
        }
        return null;
    }

    private String decode(String value) {
        try {
            return java.net.URLDecoder.decode(value, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return value;
        }
    }
}
