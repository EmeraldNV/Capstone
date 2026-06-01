package abdellah.ecommerce.api.dto.admin;

import java.time.Instant;
import java.util.Set;

public record AdminUserResponse(
        Long id,
        String email,
        String status,
        Boolean emailVerified,
        Set<String> roles,
        Instant createdAt,
        Instant lastLoginAt
) {
}
