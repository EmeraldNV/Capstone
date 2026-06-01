package abdellah.ecommerce.api.dto.product;

public record ProductImageResponse(
        Long id,
        String imageUrl,
        String altText,
        Boolean primary,
        Integer sortOrder
) {
}
