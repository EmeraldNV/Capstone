package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "audit_log")
public class AuditLog extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private AppUser actorUser;

    @NotBlank
    @Size(max = 50)
    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @NotBlank
    @Size(max = 100)
    @Column(name = "entity_name", nullable = false, length = 100)
    private String entityName;

    @NotNull
    @Column(name = "entity_id", nullable = false)
    private Long entityId;

    @Column(name = "old_data", columnDefinition = "text")
    private String oldData;

    @Column(name = "new_data", columnDefinition = "text")
    private String newData;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "text")
    private String userAgent;
}
