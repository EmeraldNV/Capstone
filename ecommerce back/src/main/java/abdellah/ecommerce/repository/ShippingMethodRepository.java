package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.ShippingMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, Long> {

    Optional<ShippingMethod> findByCodeIgnoreCase(String code);

    Optional<ShippingMethod> findFirstByActiveTrueOrderByCreatedAtAsc();
}
