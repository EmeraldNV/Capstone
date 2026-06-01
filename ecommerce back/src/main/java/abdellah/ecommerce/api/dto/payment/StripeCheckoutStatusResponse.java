package abdellah.ecommerce.api.dto.payment;

import java.math.BigDecimal;

public record StripeCheckoutStatusResponse(
        String sessionId,
        String status,
        String checkoutType,
        String orderNumber,
        String paymentIntentId,
        String customerEmail,
        BigDecimal amountTotal,
        String currencyCode,
        String message
) {
}
