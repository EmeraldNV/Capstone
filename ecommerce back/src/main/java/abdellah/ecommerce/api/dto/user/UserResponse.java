package abdellah.ecommerce.api.dto.user;

import java.time.Instant;
import java.util.Set;

public record UserResponse(
        Long id,
        String email,
        String status,
        Boolean emailVerified,
        Set<String> roles,
        Instant createdAt
) {
}
