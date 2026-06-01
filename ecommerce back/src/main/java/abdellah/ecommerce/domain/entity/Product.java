package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import abdellah.ecommerce.domain.enums.AgeGroup;
import abdellah.ecommerce.domain.enums.ProductGender;
import abdellah.ecommerce.domain.enums.Season;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "product")
public class Product extends TimestampedEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotBlank
    @Size(max = 80)
    @Column(name = "product_code", nullable = false, unique = true, length = 80)
    private String productCode;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, unique = true, length = 160)
    private String slug;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 500)
    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(columnDefinition = "text")
    private String description;

    @NotBlank
    @Size(max = 80)
    @Column(name = "sport_type", nullable = false, length = 80)
    private String sportType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductGender gender = ProductGender.UNISEX;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 20)
    private AgeGroup ageGroup = AgeGroup.ADULT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Season season;

    @Size(max = 120)
    @Column(length = 120)
    private String material;

    @Column(name = "care_instructions", columnDefinition = "text")
    private String careInstructions;

    @NotNull
    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "list_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal listPrice = BigDecimal.ZERO;

    @Digits(integer = 10, fraction = 2)
    @DecimalMin("0.00")
    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @NotBlank
    @Size(max = 3)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode = "EUR";

    @NotNull
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @NotNull
    @Column(nullable = false)
    private Boolean taxable = Boolean.TRUE;

    @NotNull
    @Column(name = "is_ipf_approved", nullable = false)
    private Boolean ipfApproved = Boolean.FALSE;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;

    @OneToMany(mappedBy = "product")
    private Set<ProductVariant> variants = new HashSet<>();

    // La relazione e many-to-many: una taglia puo essere condivisa da piu prodotti,
    // e lo stesso prodotto puo offrire piu taglie. La tabella di join normalizza il modello.
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "product_taglie",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "taglia_id")
    )
    @OrderBy("nome ASC, codice ASC")
    private Set<Taglia> taglie = new HashSet<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, createdAt ASC")
    private Set<ProductImage> images = new HashSet<>();

    @OneToMany(mappedBy = "product")
    private Set<CustomerOrderItem> orderItems = new HashSet<>();
}
