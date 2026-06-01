package abdellah.ecommerce.api.dto.admin;

import java.time.Instant;

public record AdminAuditLogResponse(
        Long id,
        String actionType,
        String entityName,
        Long entityId,
        String actorEmail,
        String ipAddress,
        String userAgent,
        String oldData,
        String newData,
        Instant createdAt
) {
}
