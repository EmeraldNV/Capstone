package abdellah.ecommerce.api.dto.product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import abdellah.ecommerce.api.dto.taglia.TagliaResponse;

public record ProductDetailResponse(
        Long id,
        String productCode,
        String slug,
        String name,
        String shortDescription,
        String description,
        String sportType,
        String gender,
        String ageGroup,
        String season,
        String material,
        String careInstructions,
        Boolean taxable,
        Boolean ipfApproved,
        Boolean active,
        BigDecimal listPrice,
        BigDecimal salePrice,
        BigDecimal effectivePrice,
        String currencyCode,
        Integer stockQuantity,
        Long brandId,
        String brandName,
        Long categoryId,
        String categoryName,
        String categorySlug,
        List<TagliaResponse> taglie,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {
}
