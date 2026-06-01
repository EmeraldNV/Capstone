package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    boolean existsByProductCodeIgnoreCase(String productCode);

    boolean existsByProductCodeIgnoreCaseAndIdNot(String productCode, Long id);

    Optional<Product> findBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = {"brand", "category", "images", "taglie"})
    Optional<Product> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"brand", "category", "images", "taglie"})
    Optional<Product> findWithDetailsBySlugIgnoreCase(String slug);

    @EntityGraph(attributePaths = {"brand", "category", "images", "taglie"})
    List<Product> findAllByActiveTrueOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"brand", "category", "images", "taglie"})
    List<Product> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"brand", "category", "images", "taglie"})
    Page<Product> findAllByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "category", "images", "taglie"})
    Page<Product> findDistinctByTaglie_IdAndActiveTrueOrderByCreatedAtDesc(Long tagliaId, Pageable pageable);
}
