package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.CheckoutType;
import abdellah.ecommerce.domain.enums.StripeCheckoutStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
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

@Getter
@Setter
@Entity
@Table(name = "stripe_checkout_session")
public class StripeCheckoutSession extends TimestampedEntity {

    @NotBlank
    @Size(max = 120)
    @Column(name = "session_id", nullable = false, unique = true, length = 120)
    private String sessionId;

    @Size(max = 120)
    @Column(name = "payment_intent_id", unique = true, length = 120)
    private String paymentIntentId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "checkout_type", nullable = false, length = 20)
    private CheckoutType checkoutType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StripeCheckoutStatus status = StripeCheckoutStatus.PENDING;

    @NotBlank
    @Size(max = 255)
    @Column(name = "customer_email", nullable = false, length = 255)
    private String customerEmail;

    @Size(max = 255)
    @Column(name = "customer_name", length = 255)
    private String customerName;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @Column(name = "amount_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountTotal;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EUR";

    @Column(name = "success_url", nullable = false, length = 500)
    private String successUrl;

    @Column(name = "cancel_url", nullable = false, length = 500)
    private String cancelUrl;

    @Column(name = "payload_json", columnDefinition = "text")
    private String payloadJson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_order_id", unique = true)
    private CustomerOrder customerOrder;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "failure_reason", columnDefinition = "text")
    private String failureReason;

    @Column(name = "receipt_email_sent_at")
    private Instant receiptEmailSentAt;
}
