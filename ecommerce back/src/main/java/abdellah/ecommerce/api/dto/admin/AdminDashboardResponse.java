package abdellah.ecommerce.api.dto.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
        LocalDate from,
        LocalDate to,
        String currencyCode,
        BigDecimal totalRevenue,
        Long totalSales,
        Long totalCustomers,
        Long totalTransactions,
        BigDecimal averageTicket,
        List<SalesTrendPointResponse> salesTrend,
        List<CategorySalesResponse> categoryBreakdown,
        List<TopProductResponse> topProducts
) {
}
