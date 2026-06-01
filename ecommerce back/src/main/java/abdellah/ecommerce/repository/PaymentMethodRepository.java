package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    Optional<PaymentMethod> findByMethodCodeIgnoreCase(String methodCode);
}
