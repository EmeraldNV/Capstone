package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.CartStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "cart")
public class Cart extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @Column(name = "guest_token", unique = true)
    private UUID guestToken;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CartStatus status = CartStatus.ACTIVE;

    @NotNull
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EUR";

    @NotNull
    @UpdateTimestamp
    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @OneToMany(mappedBy = "cart", orphanRemoval = true)
    private Set<CartItem> items = new HashSet<>();

    @OneToOne(mappedBy = "cart", fetch = FetchType.LAZY)
    private CustomerOrder order;

    @AssertTrue
    public boolean hasOwner() {
        return user != null || guestToken != null;
    }
}
