package abdellah.ecommerce.api.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record AssignRolesRequest(
        @NotEmpty Set<@NotBlank @Size(max = 50) String> roleCodes
) {
}
