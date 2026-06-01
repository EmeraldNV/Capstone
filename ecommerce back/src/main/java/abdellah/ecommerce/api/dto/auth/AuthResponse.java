package abdellah.ecommerce.api.dto.auth;

import abdellah.ecommerce.api.dto.user.UserResponse;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
