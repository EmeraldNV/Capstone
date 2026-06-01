package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.payment.StripeCartCheckoutRequest;
import abdellah.ecommerce.api.dto.payment.StripeCheckoutResponse;
import abdellah.ecommerce.api.dto.payment.StripeCheckoutStatusResponse;
import abdellah.ecommerce.api.dto.payment.StripeQuickBuyRequest;
import abdellah.ecommerce.exception.ForbiddenApiException;
import abdellah.ecommerce.security.UserPrincipal;
import abdellah.ecommerce.service.StripeCheckoutService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments/stripe")
public class StripeCheckoutController {

    private final StripeCheckoutService service;

    public StripeCheckoutController(StripeCheckoutService service) {
        this.service = service;
    }

    @PostMapping("/cart")
    public ResponseEntity<StripeCheckoutResponse> createCartCheckout(@AuthenticationPrincipal UserPrincipal principal,
                                                                     @Valid @RequestBody StripeCartCheckoutRequest request) {
        return ResponseEntity.ok(service.createCartCheckout(resolveUserId(principal), resolveEmail(principal), request));
    }

    @PostMapping("/quick-buy")
    public ResponseEntity<StripeCheckoutResponse> createQuickBuyCheckout(@AuthenticationPrincipal UserPrincipal principal,
                                                                         @Valid @RequestBody StripeQuickBuyRequest request) {
        return ResponseEntity.ok(service.createQuickBuyCheckout(resolveUserId(principal), resolveEmail(principal), request));
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<StripeCheckoutStatusResponse> getSessionStatus(@AuthenticationPrincipal UserPrincipal principal,
                                                                         @PathVariable String sessionId) {
        resolveUserId(principal);
        return ResponseEntity.ok(service.getCheckoutStatus(sessionId, resolveEmail(principal)));
    }

    private Long resolveUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenApiException("AUTH_REQUIRED", "Authentication is required to proceed with checkout.");
        }
        return principal.getId();
    }

    private String resolveEmail(UserPrincipal principal) {
        if (principal == null) {
            throw new ForbiddenApiException("AUTH_REQUIRED", "Authentication is required to proceed with checkout.");
        }
        return principal.getEmail();
    }
}
