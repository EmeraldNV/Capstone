package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.CustomerOrder;
import abdellah.ecommerce.domain.enums.OrderPaymentStatus;
import abdellah.ecommerce.domain.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    Optional<CustomerOrder> findByOrderNumberIgnoreCase(String orderNumber);

    boolean existsByOrderNumberIgnoreCase(String orderNumber);

    @EntityGraph(attributePaths = {
            "customerProfile",
            "customerProfile.user",
            "items",
            "items.product",
            "items.product.category",
            "paymentTransactions",
            "paymentTransactions.paymentMethod"
    })
    @Query("""
            select distinct co
            from CustomerOrder co
            where (:from is null or co.placedAt >= :from)
              and (:toExclusive is null or co.placedAt < :toExclusive)
              and (:categoryId is null or exists (
                    select 1
                    from CustomerOrderItem item
                    where item.customerOrder = co
                      and item.product.category.id = :categoryId
              ))
              and (:paymentMethodCode is null or exists (
                    select 1
                    from PaymentTransaction tx
                    where tx.customerOrder = co
                      and lower(tx.paymentMethod.methodCode) = lower(:paymentMethodCode)
              ))
              and (:orderStatus is null or co.orderStatus = :orderStatus)
              and (:paymentStatus is null or co.paymentStatus = :paymentStatus)
            order by co.placedAt asc, co.id asc
            """)
    List<CustomerOrder> findAdminDashboardOrders(Instant from,
                                                 Instant toExclusive,
                                                 Long categoryId,
                                                 String paymentMethodCode,
                                                 OrderStatus orderStatus,
                                                 OrderPaymentStatus paymentStatus);
}
