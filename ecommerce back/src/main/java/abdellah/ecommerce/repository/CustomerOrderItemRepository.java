package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.CustomerOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderItemRepository extends JpaRepository<CustomerOrderItem, Long> {
}
