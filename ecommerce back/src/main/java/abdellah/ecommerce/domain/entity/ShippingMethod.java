package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "shipping_method")
public class ShippingMethod extends TimestampedEntity {

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "free_shipping_threshold", precision = 12, scale = 2)
    private BigDecimal freeShippingThreshold;

    @NotNull
    @Min(0)
    @Column(name = "delivery_days_min", nullable = false)
    private Integer deliveryDaysMin = 0;

    @NotNull
    @Min(0)
    @Column(name = "delivery_days_max", nullable = false)
    private Integer deliveryDaysMax = 0;

    @NotNull
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "shippingMethod")
    private Set<CustomerOrder> orders = new HashSet<>();

    @OneToMany(mappedBy = "shippingMethod")
    private Set<Shipment> shipments = new HashSet<>();

    @AssertTrue
    public boolean isValidDeliveryWindow() {
        return deliveryDaysMax == null || deliveryDaysMin == null || deliveryDaysMax >= deliveryDaysMin;
    }

    @AssertTrue
    public boolean isValidFreeShippingThreshold() {
        return freeShippingThreshold == null || freeShippingThreshold.signum() >= 0;
    }
}
