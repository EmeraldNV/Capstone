package abdellah.ecommerce.api.dto.account;

public record AddressResponse(
        String label,
        String recipientName,
        String companyName,
        String phone,
        String line1,
        String line2,
        String city,
        String stateRegion,
        String postalCode,
        String countryCode,
        boolean defaultAddress
) {
}
