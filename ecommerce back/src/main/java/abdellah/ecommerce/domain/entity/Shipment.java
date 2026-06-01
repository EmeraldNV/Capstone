package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.ShipmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "shipment")
public class Shipment extends TimestampedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_method_id", nullable = false)
    private ShippingMethod shippingMethod;

    @NotBlank
    @Size(max = 120)
    @Column(name = "carrier_name", nullable = false, length = 120)
    private String carrierName;

    @NotBlank
    @Size(max = 120)
    @Column(name = "tracking_number", nullable = false, length = 120)
    private String trackingNumber;

    @Size(max = 500)
    @Column(name = "tracking_url", length = 500)
    private String trackingUrl;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false, length = 30)
    private ShipmentStatus shipmentStatus = ShipmentStatus.PENDING;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "estimated_delivery_at")
    private Instant estimatedDeliveryAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @OneToMany(mappedBy = "shipment", orphanRemoval = true)
    private Set<ShipmentItem> items = new HashSet<>();

    @OneToMany(mappedBy = "shipment", orphanRemoval = true)
    private Set<ShipmentEvent> events = new HashSet<>();
}
