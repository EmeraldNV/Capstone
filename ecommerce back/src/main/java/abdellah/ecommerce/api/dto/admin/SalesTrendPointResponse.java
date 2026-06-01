package abdellah.ecommerce.api.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesTrendPointResponse(
        LocalDate date,
        BigDecimal revenue,
        Long orders
) {
}
