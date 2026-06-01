package abdellah.ecommerce.bootstrap;

import abdellah.ecommerce.config.BootstrapAdminProperties;
import abdellah.ecommerce.domain.entity.AppUser;
import abdellah.ecommerce.domain.entity.Role;
import abdellah.ecommerce.domain.entity.UserRole;
import abdellah.ecommerce.domain.enums.AppUserStatus;
import abdellah.ecommerce.repository.AppUserRepository;
import abdellah.ecommerce.repository.RoleRepository;
import abdellah.ecommerce.repository.UserRoleRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class DataBootstrapRunner implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties bootstrapAdminProperties;

    public DataBootstrapRunner(RoleRepository roleRepository,
                               AppUserRepository appUserRepository,
                               UserRoleRepository userRoleRepository,
                               PasswordEncoder passwordEncoder,
                               BootstrapAdminProperties bootstrapAdminProperties) {
        this.roleRepository = roleRepository;
        this.appUserRepository = appUserRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminProperties = bootstrapAdminProperties;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureRole("CUSTOMER", "Customer");
        ensureRole("STAFF", "Staff");
        Role admin = ensureRole("ADMIN", "Administrator");

        if (bootstrapAdminProperties.isEnabled()
                && StringUtils.hasText(bootstrapAdminProperties.getEmail())
                && StringUtils.hasText(bootstrapAdminProperties.getPassword())
                ) {
            AppUser user = appUserRepository.findByEmailIgnoreCase(bootstrapAdminProperties.getEmail())
                    .orElseGet(() -> {
                        AppUser newUser = new AppUser();
                        newUser.setEmail(bootstrapAdminProperties.getEmail().trim().toLowerCase());
                        newUser.setPasswordHash(passwordEncoder.encode(bootstrapAdminProperties.getPassword()));
                        newUser.setStatus(AppUserStatus.ACTIVE);
                        newUser.setEmailVerified(Boolean.TRUE);
                        return appUserRepository.saveAndFlush(newUser);
                    });

            if (!userRoleRepository.existsByUser_IdAndRole_Id(user.getId(), admin.getId())) {
                UserRole userRole = new UserRole();
                userRole.setUser(user);
                userRole.setRole(admin);
                userRoleRepository.save(userRole);
            }
        }
    }

    private Role ensureRole(String code, String name) {
        return roleRepository.findByRoleCodeIgnoreCase(code)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRoleCode(code);
                    role.setRoleName(name);
                    return roleRepository.save(role);
                });
    }
}
