package abdellah.ecommerce.api.controller;

import abdellah.ecommerce.api.dto.admin.AdminDashboardFilterOptionsResponse;
import abdellah.ecommerce.api.dto.admin.AdminDashboardFiltersRequest;
import abdellah.ecommerce.api.dto.admin.AdminDashboardResponse;
import abdellah.ecommerce.service.AdminDashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/filters")
    public AdminDashboardFilterOptionsResponse filters() {
        return adminDashboardService.getFilterOptions();
    }

    @GetMapping
    public AdminDashboardResponse dashboard(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String paymentMethodCode,
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) String paymentStatus) {
        return adminDashboardService.getDashboard(
                new AdminDashboardFiltersRequest(from, to, categoryId, paymentMethodCode, orderStatus, paymentStatus)
        );
    }
}
