package abdellah.ecommerce.api.dto.admin;

import abdellah.ecommerce.domain.enums.AppUserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record  AdminCreateUserRequest(
        @Email @NotBlank @Size(max = 255) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        AppUserStatus status,
        Boolean emailVerified,
        @NotEmpty Set<@NotBlank @Size(max = 50) String> roleCodes
) {
}
