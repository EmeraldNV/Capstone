package abdellah.ecommerce.api.dto.product;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ProductImageOrderRequest(
        @NotEmpty List<Long> imageIds
) {
}
