package abdellah.ecommerce.api.dto.payment;

import java.math.BigDecimal;

public record StripeCheckoutResponse(
        String sessionId,
        String url,
        String status,
        BigDecimal amountTotal,
        String currencyCode
) {
}
