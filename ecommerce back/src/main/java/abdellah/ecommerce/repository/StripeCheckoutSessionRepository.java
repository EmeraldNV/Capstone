package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.StripeCheckoutSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface StripeCheckoutSessionRepository extends JpaRepository<StripeCheckoutSession, Long> {

    Optional<StripeCheckoutSession> findBySessionId(String sessionId);

    Optional<StripeCheckoutSession> findByPaymentIntentId(String paymentIntentId);

    @EntityGraph(attributePaths = {"customerOrder", "customerOrder.items"})
    Optional<StripeCheckoutSession> findWithOrderBySessionId(String sessionId);
}
