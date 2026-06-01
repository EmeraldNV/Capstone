package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.payment.PaymentStatusUpdate;
import abdellah.ecommerce.domain.entity.CustomerOrder;
import abdellah.ecommerce.domain.entity.CustomerOrderItem;
import abdellah.ecommerce.domain.entity.StripeCheckoutSession;
import abdellah.ecommerce.repository.StripeCheckoutSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PaymentNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationService.class);

    private final StripeCheckoutSessionRepository checkoutSessionRepository;
    private final PaymentWebSocketRegistry paymentWebSocketRegistry;
    private final ResendEmailService resendEmailService;

    public PaymentNotificationService(StripeCheckoutSessionRepository checkoutSessionRepository,
                                      PaymentWebSocketRegistry paymentWebSocketRegistry,
                                      ResendEmailService resendEmailService) {
        this.checkoutSessionRepository = checkoutSessionRepository;
        this.paymentWebSocketRegistry = paymentWebSocketRegistry;
        this.resendEmailService = resendEmailService;
    }

    public Optional<PaymentStatusUpdate> findUpdate(String sessionId) {
        return checkoutSessionRepository.findWithOrderBySessionId(sessionId)
                .map(this::toUpdate);
    }

    public void publishCurrentState(String sessionId) {
        findUpdate(sessionId).ifPresent(paymentWebSocketRegistry::broadcast);
    }

    public void publishFinalState(String sessionId) {
        checkoutSessionRepository.findWithOrderBySessionId(sessionId).ifPresent(session -> {
            PaymentStatusUpdate update = toUpdate(session);
            paymentWebSocketRegistry.broadcast(update);
            if ("COMPLETED".equalsIgnoreCase(update.status()) && session.getReceiptEmailSentAt() == null) {
                if (sendReceiptEmail(update)) {
                    session.setReceiptEmailSentAt(Instant.now());
                    checkoutSessionRepository.save(session);
                }
            }
        });
    }

    public PaymentStatusUpdate snapshotFromSession(StripeCheckoutSession session) {
        return toUpdate(session);
    }

    private PaymentStatusUpdate toUpdate(StripeCheckoutSession session) {
        CustomerOrder order = session.getCustomerOrder();
        List<String> items = new ArrayList<>();
        if (order != null && order.getItems() != null) {
            items = order.getItems().stream()
                    .sorted((left, right) -> {
                        if (left.getCreatedAt() == null && right.getCreatedAt() == null) {
                            return 0;
                        }
                        if (left.getCreatedAt() == null) {
                            return 1;
                        }
                        if (right.getCreatedAt() == null) {
                            return -1;
                        }
                        return left.getCreatedAt().compareTo(right.getCreatedAt());
                    })
                    .map(this::formatOrderItem)
                    .toList();
        }

        String message = switch (session.getStatus()) {
            case COMPLETED -> "Payment completed successfully.";
            case CANCELED -> "Payment canceled.";
            case EXPIRED -> "Checkout session expired.";
            case FAILED -> "Payment failed.";
            case PENDING -> "Payment session pending.";
        };

        return new PaymentStatusUpdate(
                "PAYMENT_STATUS_UPDATED",
                session.getSessionId(),
                session.getStatus().name(),
                session.getCheckoutType().name(),
                order == null ? null : order.getOrderNumber(),
                session.getPaymentIntentId(),
                session.getCustomerEmail(),
                session.getAmountTotal(),
                session.getCurrencyCode(),
                message,
                session.getUpdatedAt() == null ? Instant.now() : session.getUpdatedAt(),
                items
        );
    }

    private String formatOrderItem(CustomerOrderItem item) {
        StringBuilder builder = new StringBuilder();
        builder.append(item.getProductNameSnapshot());
        if (item.getVariantSnapshot() != null && !item.getVariantSnapshot().isBlank()) {
            builder.append(" - ").append(item.getVariantSnapshot().trim());
        }
        builder.append(" x").append(item.getQuantity());
        builder.append(" (").append(item.getLineTotal()).append(' ').append(item.getCurrencyCode()).append(')');
        return builder.toString();
    }

    private boolean sendReceiptEmail(PaymentStatusUpdate update) {
        try {
            resendEmailService.sendPaymentReceiptEmail(
                    update.customerEmail(),
                    update.orderNumber(),
                    update.sessionId(),
                    update.paymentIntentId(),
                    update.amountTotal(),
                    update.currencyCode(),
                    update.items(),
                    update.updatedAt()
            );
            return true;
        } catch (Exception ex) {
            log.warn("Unable to send payment receipt email for session {}: {}", update.sessionId(), ex.getMessage(), ex);
            return false;
        }
    }
}
