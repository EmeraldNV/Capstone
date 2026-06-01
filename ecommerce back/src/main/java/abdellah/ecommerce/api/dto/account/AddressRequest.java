package abdellah.ecommerce.api.dto.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(
        @NotBlank @Size(max = 100) String label,
        @NotBlank @Size(max = 150) String recipientName,
        @Size(max = 150) String companyName,
        @Size(max = 30) String phone,
        @NotBlank @Size(max = 200) String line1,
        @Size(max = 200) String line2,
        @NotBlank @Size(max = 100) String city,
        @Size(max = 100) String stateRegion,
        @NotBlank @Size(max = 20) String postalCode,
        @NotBlank @Pattern(regexp = "^[A-Z]{2}$", message = "Country code must contain two uppercase letters.") String countryCode
) {
}
