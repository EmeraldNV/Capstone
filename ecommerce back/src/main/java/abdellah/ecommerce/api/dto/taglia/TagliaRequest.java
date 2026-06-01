package abdellah.ecommerce.api.dto.taglia;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TagliaRequest(
        @NotBlank @Size(max = 120) String nome,
        @NotBlank @Size(max = 40) @Pattern(regexp = "^[A-Z0-9_\\-]+$") String codice
) {
}
