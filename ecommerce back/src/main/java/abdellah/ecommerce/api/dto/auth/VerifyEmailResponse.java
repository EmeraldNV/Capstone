package abdellah.ecommerce.api.dto.auth;

public record VerifyEmailResponse(
        String message,
        boolean verified
) {
}
