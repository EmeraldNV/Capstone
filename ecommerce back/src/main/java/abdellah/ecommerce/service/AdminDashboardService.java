package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.admin.AdminDashboardFilterOptionsResponse;
import abdellah.ecommerce.api.dto.admin.AdminDashboardFiltersRequest;
import abdellah.ecommerce.api.dto.admin.AdminDashboardResponse;
import abdellah.ecommerce.api.dto.admin.AdminFilterOptionResponse;
import abdellah.ecommerce.api.dto.admin.CategorySalesResponse;
import abdellah.ecommerce.api.dto.admin.SalesTrendPointResponse;
import abdellah.ecommerce.api.dto.admin.TopProductResponse;
import abdellah.ecommerce.domain.entity.Category;
import abdellah.ecommerce.domain.entity.CustomerOrder;
import abdellah.ecommerce.domain.entity.CustomerOrderItem;
import abdellah.ecommerce.domain.entity.PaymentMethod;
import abdellah.ecommerce.domain.entity.Product;
import abdellah.ecommerce.domain.enums.OrderPaymentStatus;
import abdellah.ecommerce.domain.enums.OrderStatus;
import abdellah.ecommerce.repository.CategoryRepository;
import abdellah.ecommerce.repository.CustomerOrderRepository;
import abdellah.ecommerce.repository.PaymentMethodRepository;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminDashboardService {

    private final CustomerOrderRepository customerOrderRepository;
    private final CategoryRepository categoryRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    public AdminDashboardService(CustomerOrderRepository customerOrderRepository,
                                 CategoryRepository categoryRepository,
                                 PaymentMethodRepository paymentMethodRepository) {
        this.customerOrderRepository = customerOrderRepository;
        this.categoryRepository = categoryRepository;
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(AdminDashboardFiltersRequest filters) {
        LocalDate end = filters != null && filters.to() != null ? filters.to() : LocalDate.now(ZoneOffset.UTC);
        LocalDate start = filters != null && filters.from() != null ? filters.from() : end.minusDays(30);
        Instant from = start.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toExclusive = end.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        Long categoryId = filters == null ? null : filters.categoryId();
        String paymentMethodCode = normalize(filters == null ? null : filters.paymentMethodCode());
        OrderStatus orderStatus = parseOrderStatus(filters == null ? null : filters.orderStatus());
        OrderPaymentStatus paymentStatus = parsePaymentStatus(filters == null ? null : filters.paymentStatus());

        List<CustomerOrder> orders = customerOrderRepository.findAll(
                adminDashboardSpecification(from, toExclusive, categoryId, paymentMethodCode, orderStatus, paymentStatus),
                Sort.by(Sort.Order.asc("placedAt"), Sort.Order.asc("id"))
        );

        String currency = orders.stream()
                .map(CustomerOrder::getCurrencyCode)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse("EUR");

        BigDecimal totalRevenue = orders.stream()
                .map(CustomerOrder::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalSales = orders.size();
        long totalCustomers = orders.stream()
                .map(order -> order.getCustomerProfile().getUser().getId())
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        long totalTransactions = orders.stream()
                .mapToLong(order -> order.getPaymentTransactions().size())
                .sum();
        BigDecimal averageTicket = totalSales == 0
                ? BigDecimal.ZERO
                : totalRevenue.divide(BigDecimal.valueOf(totalSales), 2, RoundingMode.HALF_UP);

        Map<LocalDate, SalesTrendAccumulator> trendMap = new LinkedHashMap<>();
        Map<String, CategoryAccumulator> categoryMap = new LinkedHashMap<>();
        Map<Long, ProductAccumulator> productMap = new LinkedHashMap<>();

        for (CustomerOrder order : orders) {
            LocalDate day = order.getPlacedAt() == null
                    ? start
                    : order.getPlacedAt().atZone(ZoneOffset.UTC).toLocalDate();
            trendMap.computeIfAbsent(day, SalesTrendAccumulator::new).addOrder(order);

            for (CustomerOrderItem item : order.getItems()) {
                Product product = item.getProduct();
                Category category = product == null ? null : product.getCategory();
                String categoryKey = category == null ? "uncategorized" : String.valueOf(category.getId());
                String categoryName = category == null ? "Senza categoria" : category.getName();
                categoryMap.computeIfAbsent(categoryKey, key -> new CategoryAccumulator(category == null ? null : category.getId(), categoryName))
                        .addItem(item);

                if (product != null) {
                    productMap.computeIfAbsent(product.getId(),
                                    id -> new ProductAccumulator(product.getId(), product.getName(), product.getSlug()))
                            .addItem(item);
                }
            }
        }

        List<SalesTrendPointResponse> salesTrend = new ArrayList<>();
        trendMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> salesTrend.add(entry.getValue().toResponse()));

        List<CategorySalesResponse> categoryBreakdown = categoryMap.values().stream()
                .sorted(Comparator.comparing(CategoryAccumulator::revenue).reversed())
                .map(CategoryAccumulator::toResponse)
                .toList();

        List<TopProductResponse> topProducts = productMap.values().stream()
                .sorted(Comparator.comparing(ProductAccumulator::revenue).reversed())
                .limit(10)
                .map(ProductAccumulator::toResponse)
                .toList();

        return new AdminDashboardResponse(
                start,
                end,
                currency,
                totalRevenue,
                totalSales,
                totalCustomers,
                totalTransactions,
                averageTicket,
                salesTrend,
                categoryBreakdown,
                topProducts
        );
    }

    @Transactional(readOnly = true)
    public AdminDashboardFilterOptionsResponse getFilterOptions() {
        List<AdminFilterOptionResponse> categories = categoryRepository.findAllByActiveTrueOrderBySortOrderAscNameAsc()
                .stream()
                .map(category -> new AdminFilterOptionResponse(String.valueOf(category.getId()), category.getName()))
                .toList();

        List<AdminFilterOptionResponse> paymentMethods = paymentMethodRepository.findAll()
                .stream()
                .filter(method -> Boolean.TRUE.equals(method.getActive()))
                .map(method -> new AdminFilterOptionResponse(method.getMethodCode(), method.getMethodName()))
                .toList();

        return new AdminDashboardFilterOptionsResponse(
                categories,
                paymentMethods,
                List.of(OrderStatus.PLACED.name(), OrderStatus.CONFIRMED.name(), OrderStatus.SHIPPED.name(),
                        OrderStatus.DELIVERED.name(), OrderStatus.CANCELLED.name()),
                List.of(OrderPaymentStatus.PENDING.name(), OrderPaymentStatus.AUTHORIZED.name(),
                        OrderPaymentStatus.PAID.name(), OrderPaymentStatus.REFUNDED.name(), OrderPaymentStatus.FAILED.name())
        );
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    private Specification<CustomerOrder> adminDashboardSpecification(Instant from,
                                                                     Instant toExclusive,
                                                                     Long categoryId,
                                                                     String paymentMethodCode,
                                                                     OrderStatus orderStatus,
                                                                     OrderPaymentStatus paymentStatus) {
        return (root, query, criteriaBuilder) -> {
            query.distinct(true);

            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("placedAt"), from));
            }
            if (toExclusive != null) {
                predicates.add(criteriaBuilder.lessThan(root.get("placedAt"), toExclusive));
            }
            if (categoryId != null) {
                var itemsJoin = root.join("items", JoinType.LEFT);
                var productJoin = itemsJoin.join("product", JoinType.LEFT);
                var categoryJoin = productJoin.join("category", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(categoryJoin.get("id"), categoryId));
            }
            if (paymentMethodCode != null) {
                var transactionJoin = root.join("paymentTransactions", JoinType.LEFT);
                var paymentMethodJoin = transactionJoin.join("paymentMethod", JoinType.LEFT);
                predicates.add(criteriaBuilder.equal(paymentMethodJoin.get("methodCode"), paymentMethodCode));
            }
            if (orderStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("orderStatus"), orderStatus));
            }
            if (paymentStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("paymentStatus"), paymentStatus));
            }

            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private OrderStatus parseOrderStatus(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return OrderStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private OrderPaymentStatus parsePaymentStatus(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        try {
            return OrderPaymentStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static final class SalesTrendAccumulator {
        private final LocalDate date;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long orders = 0L;

        private SalesTrendAccumulator(LocalDate date) {
            this.date = date;
        }

        private void addOrder(CustomerOrder order) {
            orders++;
            if (order.getTotalAmount() != null) {
                revenue = revenue.add(order.getTotalAmount());
            }
        }

        private SalesTrendPointResponse toResponse() {
            return new SalesTrendPointResponse(date, revenue, orders);
        }
    }

    private static final class CategoryAccumulator {
        private final Long categoryId;
        private final String categoryName;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long quantity = 0L;

        private CategoryAccumulator(Long categoryId, String categoryName) {
            this.categoryId = categoryId;
            this.categoryName = categoryName;
        }

        private void addItem(CustomerOrderItem item) {
            quantity += item.getQuantity() == null ? 0L : item.getQuantity();
            if (item.getLineTotal() != null) {
                revenue = revenue.add(item.getLineTotal());
            }
        }

        private BigDecimal revenue() {
            return revenue;
        }

        private CategorySalesResponse toResponse() {
            return new CategorySalesResponse(categoryId, categoryName, revenue, quantity);
        }
    }

    private static final class ProductAccumulator {
        private final Long productId;
        private final String productName;
        private final String slug;
        private BigDecimal revenue = BigDecimal.ZERO;
        private long quantity = 0L;

        private ProductAccumulator(Long productId, String productName, String slug) {
            this.productId = productId;
            this.productName = productName;
            this.slug = slug;
        }

        private void addItem(CustomerOrderItem item) {
            quantity += item.getQuantity() == null ? 0L : item.getQuantity();
            if (item.getLineTotal() != null) {
                revenue = revenue.add(item.getLineTotal());
            }
        }

        private BigDecimal revenue() {
            return revenue;
        }

        private TopProductResponse toResponse() {
            return new TopProductResponse(productId, productName, slug, revenue, quantity);
        }
    }
}
