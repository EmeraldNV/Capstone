package abdellah.ecommerce.api.dto.payment;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StripeCheckoutItemRequest(
        @NotNull Long productId,
        Long productVariantId,
        @Min(1) Integer quantity,
        @Size(max = 255) String variantLabel
) {
    public Integer quantity() {
        return quantity == null ? 1 : quantity;
    }
}
