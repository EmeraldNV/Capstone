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

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "shipment_event")
public class ShipmentEvent extends TimestampedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    @NotBlank
    @Size(max = 50)
    @Column(name = "event_code", nullable = false, length = 50)
    private String eventCode;

    @Column(name = "event_description", columnDefinition = "text")
    private String eventDescription;

    @Size(max = 120)
    @Column(length = 120)
    private String location;

    @NotNull
    @Column(name = "event_at", nullable = false)
    private Instant eventAt;
}
