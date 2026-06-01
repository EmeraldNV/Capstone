package abdellah.ecommerce.api.dto.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import abdellah.ecommerce.api.dto.taglia.TagliaResponse;

public record ProductSummaryResponse(
        Long id,
        String productCode,
        String slug,
        String name,
        String shortDescription,
        BigDecimal listPrice,
        BigDecimal salePrice,
        BigDecimal effectivePrice,
        String currencyCode,
        Integer stockQuantity,
        Boolean active,
        String thumbnailUrl,
        String brandName,
        String categoryName,
        String categorySlug,
        Boolean ipfApproved,
        List<TagliaResponse> taglie,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
}
