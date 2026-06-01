package abdellah.ecommerce.api.dto.auth;

import java.time.Instant;

public record RegistrationResponse(
        String message,
        String email,
        boolean verificationRequired,
        Instant verificationExpiresAt
) {
}
