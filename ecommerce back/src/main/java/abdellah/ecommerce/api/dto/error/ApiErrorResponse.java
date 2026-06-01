package abdellah.ecommerce.api.dto.error;

import java.time.Instant;
import java.util.List;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        List<FieldErrorDetail> details
) {
}
