package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.EmailVerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenHashAndVerifiedAtIsNullAndExpiresAtAfter(String tokenHash, Instant now);
}
