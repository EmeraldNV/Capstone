package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "product_variant")
public class ProductVariant extends TimestampedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @Size(max = 80)
    @Column(length = 80, unique = true)
    private String barcode;

    @Size(max = 30)
    @Column(name = "size_code", length = 30)
    private String sizeCode;

    @Size(max = 60)
    @Column(name = "color_name", length = 60)
    private String colorName;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$")
    @Size(max = 7)
    @Column(name = "color_hex_code", length = 7)
    private String colorHexCode;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "list_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal listPrice;

    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EUR";

    @NotNull
    @Min(0)
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @NotNull
    @Min(0)
    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity = 0;

    @Min(1)
    @Column(name = "weight_grams")
    private Integer weightGrams;

    @NotNull
    @Column(nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "productVariant")
    private Set<ProductImage> images = new HashSet<>();

    @OneToMany(mappedBy = "productVariant")
    private Set<InventoryMovement> inventoryMovements = new HashSet<>();

    @OneToMany(mappedBy = "productVariant")
    private Set<CartItem> cartItems = new HashSet<>();

    @OneToMany(mappedBy = "productVariant")
    private Set<CustomerOrderItem> orderItems = new HashSet<>();

    @AssertTrue
    public boolean isSalePriceValid() {
        return salePrice == null || listPrice == null || salePrice.compareTo(listPrice) <= 0;
    }
}
