package abdellah.ecommerce.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 1, max = 72) String password
) {
}
