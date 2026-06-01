package abdellah.ecommerce.api.dto.admin;

import java.util.List;

public record AdminDashboardFilterOptionsResponse(
        List<AdminFilterOptionResponse> categories,
        List<AdminFilterOptionResponse> paymentMethods,
        List<String> orderStatuses,
        List<String> paymentStatuses
) {
}
