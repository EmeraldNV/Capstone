package abdellah.ecommerce.api.dto.category;

import java.util.List;

public record CategoryNavigationResponse(
        Long id,
        String categoryCode,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Long parentId,
        String parentSlug,
        List<CategoryNavigationResponse> children
) {
}
