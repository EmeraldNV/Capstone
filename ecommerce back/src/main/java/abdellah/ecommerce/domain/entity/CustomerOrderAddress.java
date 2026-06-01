package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.AddressType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "customer_order_address",
        uniqueConstraints = @jakarta.persistence.UniqueConstraint(columnNames = {"customer_order_id", "address_type"})
)
public class CustomerOrderAddress extends TimestampedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;

    @NotBlank
    @Size(max = 150)
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Size(max = 150)
    @Column(name = "company_name", length = 150)
    private String companyName;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @Size(max = 255)
    @Column(length = 255)
    private String email;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String line1;

    @Size(max = 200)
    @Column(length = 200)
    private String line2;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String city;

    @Size(max = 100)
    @Column(name = "state_region", length = 100)
    private String stateRegion;

    @NotBlank
    @Size(max = 20)
    @Column(name = "postal_code", nullable = false, length = 20)
    private String postalCode;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{2}$")
    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;
}
