package abdellah.ecommerce.api.dto.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 150) String companyName,
        @Size(max = 32) String taxCode,
        @Size(max = 32) String vatNumber,
        @Past LocalDate birthDate,
        @Size(max = 30) String phone,
        @NotNull Boolean marketingConsent,
        @Valid @NotNull AddressRequest address
) {
}
