package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.auth.AuthResponse;
import abdellah.ecommerce.api.dto.auth.LoginRequest;
import abdellah.ecommerce.api.dto.auth.RegisterRequest;
import abdellah.ecommerce.api.dto.auth.RegistrationResponse;
import abdellah.ecommerce.api.dto.user.UserResponse;
import abdellah.ecommerce.domain.entity.AppUser;
import abdellah.ecommerce.domain.entity.Role;
import abdellah.ecommerce.domain.entity.UserRole;
import abdellah.ecommerce.domain.enums.AppUserStatus;
import abdellah.ecommerce.exception.BadRequestApiException;
import abdellah.ecommerce.exception.ConflictApiException;
import abdellah.ecommerce.exception.ForbiddenApiException;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.AppUserRepository;
import abdellah.ecommerce.repository.RoleRepository;
import abdellah.ecommerce.repository.UserRoleRepository;
import abdellah.ecommerce.security.JwtService;
import abdellah.ecommerce.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String CUSTOMER_ROLE = "CUSTOMER";

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthService(AppUserRepository appUserRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       EmailVerificationService emailVerificationService) {
        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public RegistrationResponse register(RegisterRequest request) {
        validatePassword(request.password(), request.confirmPassword(), request.email());
        ensureEmailAvailable(request.email());

        AppUser user = new AppUser();
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(AppUserStatus.PENDING);
        user.setEmailVerified(Boolean.FALSE);
        user = appUserRepository.saveAndFlush(user);

        assignRole(user, CUSTOMER_ROLE, null);

        var expiresAt = emailVerificationService.createAndSendVerificationToken(user);
        return new RegistrationResponse(
                "Account created. Check your email to verify the account before logging in.",
                user.getEmail(),
                true,
                expiresAt
        );
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        AppUser existing = appUserRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existing != null && !Boolean.TRUE.equals(existing.getEmailVerified())) {
            throw new ForbiddenApiException("EMAIL_NOT_VERIFIED", "Please verify your email address before logging in.");
        }
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, request.password())
            );
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            AppUser user = loadUserWithRoles(principal.getId());
            return new AuthResponse(
                    jwtService.generateToken(principal),
                    "Bearer",
                    jwtService.getAccessTokenTtlSeconds(),
                    UserResponseMapper.toResponse(user)
            );
        } catch (DisabledException | LockedException ex) {
            throw new ForbiddenApiException("ACCOUNT_DISABLED", "Account is inactive or locked.");
        }
    }

    private void validatePassword(String password, String confirmPassword, String email) {
        if (!password.equals(confirmPassword)) {
            throw new BadRequestApiException("PASSWORD_MISMATCH", "Password and confirmPassword do not match.");
        }
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

    private void ensureEmailAvailable(String email) {
        if (appUserRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictApiException("EMAIL_ALREADY_EXISTS", "An account with this email already exists.");
        }
    }

    private void assignRole(AppUser user, String roleCode, AppUser assignedByUser) {
        Role role = roleRepository.findByRoleCodeIgnoreCase(roleCode)
                .orElseThrow(() -> new NotFoundApiException("ROLE_NOT_FOUND", "Role " + roleCode + " was not found."));
        if (!userRoleRepository.existsByUser_IdAndRole_Id(user.getId(), role.getId())) {
            UserRole userRole = new UserRole();
            userRole.setUser(user);
            userRole.setRole(role);
            userRole.setAssignedByUser(assignedByUser);
            userRoleRepository.save(userRole);
        }
    }

    private AppUser loadUserWithRoles(Long userId) {
        return appUserRepository.findWithRolesById(userId)
                .orElseThrow(() -> new NotFoundApiException("USER_NOT_FOUND", "User was not found."));
    }
}
