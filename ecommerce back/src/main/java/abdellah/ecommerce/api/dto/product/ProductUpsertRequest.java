package abdellah.ecommerce.api.dto.product;

import abdellah.ecommerce.domain.enums.AgeGroup;
import abdellah.ecommerce.domain.enums.ProductGender;
import abdellah.ecommerce.domain.enums.Season;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpsertRequest(
        @Size(max = 80) String productCode,
        @Size(max = 160) String slug,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 500) String shortDescription,
        String description,
        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^(FOOTWEAR_SHOES|FOOTWEAR_SOCKS|APPAREL_TSHIRT|APPAREL_CROP_TOP)$")
        String sportType,
        @NotNull ProductGender gender,
        @NotNull AgeGroup ageGroup,
        Season season,
        @Size(max = 120) String material,
        String careInstructions,
        @NotNull Boolean taxable,
        @NotNull Boolean ipfApproved,
        @NotNull Boolean active,
        @NotNull @Digits(integer = 10, fraction = 2) @DecimalMin("0.00") BigDecimal listPrice,
        @Digits(integer = 10, fraction = 2) @DecimalMin("0.00") BigDecimal salePrice,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$") String currencyCode,
        @NotNull @Min(0) Integer stockQuantity,
        Long brandId,
        @NotNull Long categoryId,
        @jakarta.validation.constraints.NotEmpty List<@NotNull Long> tagliaIds
) {
}
