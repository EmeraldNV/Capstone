package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByTransactionReferenceIgnoreCase(String transactionReference);
}
