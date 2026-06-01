package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySkuIgnoreCase(String sku);

    @EntityGraph(attributePaths = {"product"})
    List<ProductVariant> findAllByProduct_IdOrderByCreatedAtAsc(Long productId);

    @EntityGraph(attributePaths = {"product"})
    Optional<ProductVariant> findWithProductById(Long id);
}
