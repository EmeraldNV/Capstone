package abdellah.ecommerce.api.dto.account;

import java.time.Instant;
import java.time.LocalDate;

public record UserProfileResponse(
        Long userId,
        String email,
        boolean emailVerified,
        String status,
        String firstName,
        String lastName,
        String companyName,
        String taxCode,
        String vatNumber,
        LocalDate birthDate,
        String phone,
        boolean marketingConsent,
        AddressResponse address,
        Instant createdAt,
        Instant updatedAt
) {
}
