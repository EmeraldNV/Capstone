package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.OrderPaymentStatus;
import abdellah.ecommerce.domain.enums.OrderStatus;
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
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "customer_order")
public class CustomerOrder extends TimestampedEntity {

    @NotBlank
    @Size(max = 30)
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_profile_id", nullable = false)
    private CustomerProfile customerProfile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", unique = true)
    private Cart cart;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shipping_method_id", nullable = false)
    private ShippingMethod shippingMethod;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "order_status", nullable = false, length = 30)
    private OrderStatus orderStatus = OrderStatus.PLACED;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private OrderPaymentStatus paymentStatus = OrderPaymentStatus.PENDING;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "shipping_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EUR";

    @Column(name = "customer_note", columnDefinition = "text")
    private String customerNote;

    @Column(name = "placed_at")
    private Instant placedAt;

    @OneToMany(mappedBy = "customerOrder", orphanRemoval = true)
    private Set<CustomerOrderAddress> addresses = new HashSet<>();

    @OneToMany(mappedBy = "customerOrder", orphanRemoval = true)
    private Set<CustomerOrderItem> items = new HashSet<>();

    @OneToMany(mappedBy = "customerOrder")
    private Set<OrderStatusHistory> statusHistory = new HashSet<>();

    @OneToMany(mappedBy = "customerOrder")
    private Set<Shipment> shipments = new HashSet<>();

    @OneToMany(mappedBy = "customerOrder")
    private Set<PaymentTransaction> paymentTransactions = new HashSet<>();
}
