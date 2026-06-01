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
@Table(name = "customer_address")
public class CustomerAddress extends TimestampedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    private AddressType addressType;

    @Size(max = 100)
    @Column(length = 100)
    private String label;

    @NotBlank
    @Size(max = 150)
    @Column(name = "recipient_name", nullable = false, length = 150)
    private String recipientName;

    @Size(max = 150)
    @Column(name = "company_name", length = 150)
    private String companyName;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

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

    @NotNull
    @Column(name = "is_default", nullable = false)
    private Boolean defaultAddress = Boolean.FALSE;
}
