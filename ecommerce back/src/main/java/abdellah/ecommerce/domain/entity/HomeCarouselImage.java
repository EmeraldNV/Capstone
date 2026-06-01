package abdellah.ecommerce.domain.entity;

import abdellah.ecommerce.domain.base.TimestampedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "home_carousel_image")
public class HomeCarouselImage extends TimestampedEntity {

    @NotBlank
    @Size(max = 500)
    @Column(name = "image_url", nullable = false, length = 500)
    private String imageUrl;

    @Size(max = 255)
    @Column(name = "alt_text", length = 255)
    private String altText;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = Boolean.TRUE;
}
