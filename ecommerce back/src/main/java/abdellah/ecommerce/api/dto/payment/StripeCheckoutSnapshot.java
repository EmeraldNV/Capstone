package abdellah.ecommerce.api.dto.payment;

import java.math.BigDecimal;

public record StripeCheckoutSnapshot(
        Long productId,
        Long productVariantId,
        Integer quantity,
        String productCode,
        String productName,
        String variantSnapshot,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        String currencyCode
) {
}
