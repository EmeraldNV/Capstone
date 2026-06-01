package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.AppUserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "app_user")
public class AppUser extends TimestampedEntity {

    @Email
    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @NotBlank
    @Size(max = 255)
    @Column(name = "password_hash", nullable = false, length = 255)
    @JsonIgnore
    private String passwordHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppUserStatus status = AppUserStatus.ACTIVE;

    @NotNull
    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified = Boolean.FALSE;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private CustomerProfile customerProfile;

    @OneToMany(mappedBy = "user", orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();

    @OneToMany(mappedBy = "assignedByUser")
    private Set<UserRole> assignedUserRoles = new HashSet<>();

    @OneToMany(mappedBy = "createdByUser")
    private Set<InventoryMovement> inventoryMovements = new HashSet<>();

    @OneToMany(mappedBy = "changedByUser")
    private Set<OrderStatusHistory> orderStatusHistory = new HashSet<>();

    @OneToMany(mappedBy = "actorUser")
    private Set<AuditLog> auditLogs = new HashSet<>();

    @OneToMany(mappedBy = "user")
    private Set<Cart> carts = new HashSet<>();
}
