package abdellah.ecommerce.api.dto.admin;

import abdellah.ecommerce.domain.enums.AppUserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AdminUserUpdateRequest(
        @Email @Size(max = 255) String email,
        AppUserStatus status,
        Boolean emailVerified,
        Set<@Size(max = 50) String> roleCodes
) {
}
