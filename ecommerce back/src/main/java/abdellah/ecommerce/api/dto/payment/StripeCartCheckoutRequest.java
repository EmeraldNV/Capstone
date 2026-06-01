package abdellah.ecommerce.api.dto.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record StripeCartCheckoutRequest(
        @Email @NotBlank @Size(max = 255) String customerEmail,
        @NotEmpty @Valid List<StripeCheckoutItemRequest> items
) {
}
