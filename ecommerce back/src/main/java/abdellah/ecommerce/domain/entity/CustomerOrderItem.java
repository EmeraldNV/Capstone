package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

@Getter
@Setter
@Entity
@Table(name = "customer_order_item")
public class CustomerOrderItem extends TimestampedEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_order_id", nullable = false)
    private CustomerOrder customerOrder;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @NotBlank
    @Size(max = 80)
    @Column(name = "sku_snapshot", nullable = false, length = 80)
    private String skuSnapshot;

    @NotBlank
    @Size(max = 200)
    @Column(name = "product_name_snapshot", nullable = false, length = 200)
    private String productNameSnapshot;

    @Size(max = 255)
    @Column(name = "variant_snapshot", length = 255)
    private String variantSnapshot;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer quantity;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    @NotBlank
    @Pattern(regexp = "^[A-Z]{3}$")
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;
}
