package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.admin.AdminAuditLogResponse;
import abdellah.ecommerce.api.dto.admin.AdminCreateUserRequest;
import abdellah.ecommerce.api.dto.admin.AdminUserResponse;
import abdellah.ecommerce.api.dto.admin.AdminUserUpdateRequest;
import abdellah.ecommerce.api.dto.admin.AssignRolesRequest;
import abdellah.ecommerce.api.dto.admin.RoleAssignmentResponse;
import abdellah.ecommerce.api.dto.user.UserResponse;
import abdellah.ecommerce.domain.entity.AppUser;
import abdellah.ecommerce.domain.entity.AuditLog;
import abdellah.ecommerce.domain.entity.Role;
import abdellah.ecommerce.domain.entity.UserRole;
import abdellah.ecommerce.domain.enums.AppUserStatus;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.exception.ConflictApiException;
import abdellah.ecommerce.exception.ForbiddenApiException;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.AppUserRepository;
import abdellah.ecommerce.repository.AuditLogRepository;
import abdellah.ecommerce.repository.RoleRepository;
import abdellah.ecommerce.repository.UserRoleRepository;
import abdellah.ecommerce.security.UserPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public AdminUserService(AppUserRepository appUserRepository,
                            RoleRepository roleRepository,
                            UserRoleRepository userRoleRepository,
                            AuditLogRepository auditLogRepository,
                            PasswordEncoder passwordEncoder,
                            AuditLogService auditLogService) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String search) {
        List<AppUser> users = hasText(search)
                ? appUserRepository.findByEmailContainingIgnoreCaseOrderByCreatedAtDesc(search.trim())
                : appUserRepository.findAllByOrderByCreatedAtDesc();
        return users.stream().map(this::toAdminUserResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long userId) {
        return toAdminUserResponse(loadUserWithRoles(userId));
    }

    @Transactional(readOnly = true)
    public List<AdminAuditLogResponse> listRecentAuditLogs(String entityName) {
        List<AuditLog> logs = hasText(entityName)
                ? auditLogRepository.findTop100ByEntityNameOrderByCreatedAtDesc(entityName.trim())
                : auditLogRepository.findTop100ByOrderByCreatedAtDesc();
        return logs.stream().map(this::toAuditLogResponse).toList();
    }

    @Transactional
    public UserResponse createUser(AdminCreateUserRequest request, UserPrincipal actor, String ipAddress, String userAgent) {
        validatePassword(request.password(), request.email());
        if (appUserRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictApiException("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
        }

        AppUser user = new AppUser();
        user.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(request.status() == null ? AppUserStatus.ACTIVE : request.status());
        user.setEmailVerified(request.emailVerified() != null ? request.emailVerified() : Boolean.FALSE);
        user = appUserRepository.saveAndFlush(user);

        syncRoles(user, request.roleCodes(), actor, false);
        AppUser reloaded = loadUserWithRoles(user.getId());
        auditLogService.log(loadActor(actor), "CREATE", "AppUser", reloaded.getId(), null, requestedSnapshot(reloaded), ipAddress, userAgent);
        return UserResponseMapper.toResponse(reloaded);
    }

    @Transactional
    public AdminUserResponse updateUser(Long userId,
                                        AdminUserUpdateRequest request,
                                        UserPrincipal actor,
                                        String ipAddress,
                                        String userAgent) {
        AppUser user = loadUserWithRoles(userId);
        Object before = requestedSnapshot(user);

        if (hasText(request.email())) {
            String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
            if (!normalizedEmail.equalsIgnoreCase(user.getEmail()) && appUserRepository.existsByEmailIgnoreCase(normalizedEmail)) {
                throw new ConflictApiException("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
            }
            user.setEmail(normalizedEmail);
        }
        if (request.status() != null) {
            user.setStatus(request.status());
        }
        if (request.emailVerified() != null) {
            user.setEmailVerified(request.emailVerified());
        }
        if (request.roleCodes() != null) {
            syncRoles(user, request.roleCodes(), actor, true);
        }

        AppUser saved = appUserRepository.saveAndFlush(user);
        AppUser reloaded = loadUserWithRoles(saved.getId());
        auditLogService.log(loadActor(actor), "UPDATE", "AppUser", reloaded.getId(), before, requestedSnapshot(reloaded), ipAddress, userAgent);
        return toAdminUserResponse(reloaded);
    }

    @Transactional
    public AdminUserResponse deactivateUser(Long userId,
                                            UserPrincipal actor,
                                            String ipAddress,
                                            String userAgent) {
        AppUser user = loadUserWithRoles(userId);
        Object before = requestedSnapshot(user);
        user.setStatus(AppUserStatus.INACTIVE);
        user.setEmailVerified(Boolean.FALSE);
        AppUser saved = appUserRepository.saveAndFlush(user);
        AppUser reloaded = loadUserWithRoles(saved.getId());
        auditLogService.log(loadActor(actor), "DEACTIVATE", "AppUser", reloaded.getId(), before, requestedSnapshot(reloaded), ipAddress, userAgent);
        return toAdminUserResponse(reloaded);
    }

    @Transactional
    public RoleAssignmentResponse assignRoles(Long userId,
                                              AssignRolesRequest request,
                                              UserPrincipal actor,
                                              String ipAddress,
                                              String userAgent) {
        AppUser user = loadUserWithRoles(userId);
        Set<String> before = user.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        syncRoles(user, request.roleCodes(), actor, true);
        AppUser reloaded = loadUserWithRoles(userId);
        Set<String> after = reloaded.getUserRoles().stream()
                .map(ur -> ur.getRole().getRoleCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        auditLogService.log(loadActor(actor), "ASSIGN_ROLES", "AppUser", reloaded.getId(), before, after, ipAddress, userAgent);
        return new RoleAssignmentResponse(reloaded.getId(), reloaded.getEmail(), after, "Roles assigned successfully.");
    }

    private void syncRoles(AppUser user, Set<String> roleCodes, UserPrincipal actor, boolean replaceCurrentRoles) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new BadRequestApiException("ROLE_CODES_REQUIRED", "At least one role code is required.");
        }

        List<String> normalizedCodes = roleCodes.stream()
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .toList();
        if (!isAdmin(actor) && normalizedCodes.contains("ADMIN")) {
            throw new ForbiddenApiException("ROLE_ASSIGNMENT_FORBIDDEN", "Only administrators can assign the ADMIN role.");
        }

        List<Role> roles = roleRepository.findByRoleCodeIn(normalizedCodes);
        if (roles.size() != normalizedCodes.size()) {
            Set<String> found = roles.stream().map(Role::getRoleCode).map(String::toUpperCase).collect(Collectors.toSet());
            String missing = normalizedCodes.stream().filter(code -> !found.contains(code)).findFirst().orElse("UNKNOWN");
            throw new NotFoundApiException("ROLE_NOT_FOUND", "Role " + missing + " was not found.");
        }

        if (replaceCurrentRoles) {
            user.getUserRoles().clear();
            userRoleRepository.flush();
        }

        AppUser actorUser = loadActor(actor);
        for (Role role : roles) {
            if (!userRoleRepository.existsByUser_IdAndRole_Id(user.getId(), role.getId())) {
                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(role);
                userRole.setAssignedByUser(actorUser);
                userRoleRepository.save(userRole);
            }
        }
    }

    private AppUser loadUserWithRoles(Long userId) {
        return appUserRepository.findWithRolesById(userId)
                .orElseThrow(() -> new NotFoundApiException("USER_NOT_FOUND", "User with id " + userId + " was not found."));
    }

    private AdminUserResponse toAdminUserResponse(AppUser user) {
        Set<String> roles = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleCode())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getStatus().name(),
                user.getEmailVerified(),
                roles,
                user.getCreatedAt(),
                user.getLastLoginAt()
        );
    }

    private AdminAuditLogResponse toAuditLogResponse(AuditLog log) {
        return new AdminAuditLogResponse(
                log.getId(),
                log.getActionType(),
                log.getEntityName(),
                log.getEntityId(),
                log.getActorUser() == null ? null : log.getActorUser().getEmail(),
                log.getIpAddress(),
                log.getUserAgent(),
                log.getOldData(),
                log.getNewData(),
                log.getCreatedAt()
        );
    }

    private AppUser loadActor(UserPrincipal actor) {
        if (actor == null) {
            return null;
        }
        return appUserRepository.findById(actor.getId()).orElse(null);
    }

    private void validatePassword(String password, String email) {
        if (email != null && password.equalsIgnoreCase(email)) {
            throw new BadRequestApiException("PASSWORD_POLICY_VIOLATION", "Password cannot be equal to email.");
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean symbol = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!(upper && lower && digit && symbol)) {
            throw new BadRequestApiException("PASSWORD_POLICY_VIOLATION", "Password must contain uppercase, lowercase, digit and symbol.");
        }
    }

    private Object requestedSnapshot(AppUser user) {
        return java.util.Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "status", user.getStatus(),
                "emailVerified", user.getEmailVerified(),
                "roles", user.getUserRoles().stream()
                        .map(userRole -> userRole.getRole().getRoleCode())
                        .collect(Collectors.toCollection(LinkedHashSet::new))
        );
    }

    private boolean isAdmin(UserPrincipal actor) {
        return actor != null && actor.getAuthorities().stream().anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
