package abdellah.ecommerce.security;

import abdellah.ecommerce.domain.entity.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final boolean enabled;
    private final boolean locked;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String passwordHash, boolean enabled, boolean locked, List<GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.enabled = enabled;
        this.locked = locked;
        this.authorities = List.copyOf(authorities);
    }

    public static UserPrincipal from(AppUser user) {
        List<GrantedAuthority> authorities = user.getUserRoles().stream()
                .map(userRole -> userRole.getRole().getRoleCode())
                .filter(Objects::nonNull)
                .distinct()
                .map(roleCode -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + roleCode.toUpperCase()))
                .toList();
        boolean enabled = user.getStatus() == abdellah.ecommerce.domain.enums.AppUserStatus.ACTIVE
                && Boolean.TRUE.equals(user.getEmailVerified());
        boolean locked = user.getStatus() == abdellah.ecommerce.domain.enums.AppUserStatus.LOCKED;
        return new UserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), enabled, locked, authorities);
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
