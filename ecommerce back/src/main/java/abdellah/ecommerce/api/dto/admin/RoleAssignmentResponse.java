package abdellah.ecommerce.api.dto.admin;

import java.util.Set;

public record RoleAssignmentResponse(
        Long userId,
        String email,
        Set<String> assignedRoles,
        String message
) {
}
