package abdellah.ecommerce.api.dto.admin;

import java.time.LocalDate;

public record AdminDashboardFiltersRequest(
        LocalDate from,
        LocalDate to,
        Long categoryId,
        String paymentMethodCode,
        String orderStatus,
        String paymentStatus
) {
}
