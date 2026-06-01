package abdellah.ecommerce.api.dto.payment;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StripeQuickBuyRequest(
        @Email @NotBlank @Size(max = 255) String customerEmail,
        @NotNull Long productId,
        Long productVariantId,
        @Min(1) Integer quantity,
        @Size(max = 255) String variantLabel
) {
    public Integer quantity() {
        return quantity == null ? 1 : quantity;
    }
}
