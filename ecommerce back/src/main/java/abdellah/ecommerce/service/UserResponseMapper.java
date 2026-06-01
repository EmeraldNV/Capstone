package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.user.UserResponse;
import abdellah.ecommerce.domain.entity.AppUser;

import java.util.LinkedHashSet;
import java.util.Set;

public final class UserResponseMapper {

    private UserResponseMapper() {
    }

    public static UserResponse toResponse(AppUser user) {
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleCode())
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus().name(),
                user.getEmailVerified(),
                roles,
                user.getCreatedAt()
        );
    }
}
