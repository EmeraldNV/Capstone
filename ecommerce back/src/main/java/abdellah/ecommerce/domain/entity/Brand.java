package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "brand")
public class Brand extends TimestampedEntity {

    @NotBlank
    @Size(max = 60)
    @Column(name = "brand_code", nullable = false, unique = true, length = 60)
    private String brandCode;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Size(max = 255)
    @Column(name = "website_url", length = 255)
    private String websiteUrl;

    @OneToMany(mappedBy = "brand")
    private Set<Product> products = new HashSet<>();
}
