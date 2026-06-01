package abdellah.ecommerce.api.dto.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentStatusUpdate(
        String eventType,
        String sessionId,
        String status,
        String checkoutType,
        String orderNumber,
        String paymentIntentId,
        String customerEmail,
        BigDecimal amountTotal,
        String currencyCode,
        String message,
        Instant updatedAt,
        List<String> items
) {
}
