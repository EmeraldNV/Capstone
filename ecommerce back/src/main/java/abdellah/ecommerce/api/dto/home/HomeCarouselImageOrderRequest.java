package abdellah.ecommerce.api.dto.home;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record HomeCarouselImageOrderRequest(
        @NotEmpty List<Long> imageIds
) {
}
