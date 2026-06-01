package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "payment_method")
public class PaymentMethod extends TimestampedEntity {

    @NotBlank
    @Size(max = 50)
    @Column(name = "method_code", nullable = false, unique = true, length = 50)
    private String methodCode;

    @NotBlank
    @Size(max = 120)
    @Column(name = "method_name", nullable = false, length = 120)
    private String methodName;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String provider;

    @NotNull
    @Column(name = "supports_refunds", nullable = false)
    private Boolean supportsRefunds = Boolean.TRUE;

    @NotNull
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "paymentMethod")
    private Set<PaymentTransaction> paymentTransactions = new HashSet<>();
}
