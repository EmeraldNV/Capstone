package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;



@Getter
@Setter
@Entity
@Table(name = "role")
public class Role extends TimestampedEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "role_code", nullable = false, unique = true, length = 50)
    private String roleCode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "role_name", nullable = false, unique = true, length = 100)
    private String roleName;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    @OneToMany(mappedBy = "role", orphanRemoval = true)
    private Set<UserRole> userRoles = new HashSet<>();
}
