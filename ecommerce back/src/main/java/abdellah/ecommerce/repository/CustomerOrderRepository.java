package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.CustomerOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.domain.Sort;

import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long>, JpaSpecificationExecutor<CustomerOrder> {

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
    java.util.List<CustomerOrder> findAll(Specification<CustomerOrder> spec, Sort sort);
}
