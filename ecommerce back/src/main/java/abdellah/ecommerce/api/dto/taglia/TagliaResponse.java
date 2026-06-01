package abdellah.ecommerce.api.dto.taglia;

import java.time.Instant;

public record TagliaResponse(
        Long id,
        String nome,
        String codice,
        Instant createdAt,
        Instant updatedAt
) {
}
