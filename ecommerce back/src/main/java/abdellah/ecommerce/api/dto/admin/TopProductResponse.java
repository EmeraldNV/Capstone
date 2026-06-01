package abdellah.ecommerce.api.dto.admin;

import java.math.BigDecimal;

public record TopProductResponse(
        Long productId,
        String productName,
        String slug,
        BigDecimal revenue,
        Long quantity
) {
}
