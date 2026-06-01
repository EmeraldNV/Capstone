package abdellah.ecommerce.api.dto.admin;

import java.math.BigDecimal;

public record CategorySalesResponse(
        Long categoryId,
        String categoryName,
        BigDecimal revenue,
        Long quantity
) {
}
