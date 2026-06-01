package abdellah.ecommerce.domain.enums;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum ProductClassification {
    FOOTWEAR_SHOES("Footwear / Shoes"),
    FOOTWEAR_SOCKS("Footwear / Socks"),
    APPAREL_TSHIRT("Apparel / T-Shirt"),
    APPAREL_CROP_TOP("Apparel / Crop Top");

    private final String label;

    ProductClassification(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static boolean isValid(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return Arrays.stream(values()).anyMatch(item -> item.name().equals(normalized));
    }

    public static String allowedValues() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining("|"));
    }
}
