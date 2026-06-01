package abdellah.ecommerce.api.dto.home;

import java.time.Instant;

public record HomeCarouselImageResponse(
        Long id,
        String imageUrl,
        String altText,
        Integer sortOrder,
        Boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
