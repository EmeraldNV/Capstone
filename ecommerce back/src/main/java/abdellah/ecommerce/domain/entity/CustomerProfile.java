package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.CustomerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "customer_profile")
public class CustomerProfile extends TimestampedEntity {

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AppUser user;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 20)
    private CustomerType customerType = CustomerType.INDIVIDUAL;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Size(max = 150)
    @Column(name = "company_name", length = 150)
    private String companyName;

    @Size(max = 32)
    @Column(name = "tax_code", unique = true, length = 32)
    private String taxCode;

    @Size(max = 32)
    @Column(name = "vat_number", unique = true, length = 32)
    private String vatNumber;

    @Past
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Size(max = 30)
    @Column(length = 30)
    private String phone;

    @NotNull
    @Column(name = "marketing_consent", nullable = false)
    private Boolean marketingConsent = Boolean.FALSE;

    @OneToMany(mappedBy = "customerProfile", orphanRemoval = true)
    private Set<CustomerAddress> addresses = new HashSet<>();

    @OneToMany(mappedBy = "customerProfile")
    private Set<CustomerOrder> orders = new HashSet<>();
}
