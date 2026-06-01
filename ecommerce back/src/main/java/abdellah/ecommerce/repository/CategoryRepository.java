package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByCategoryCodeIgnoreCase(String categoryCode);

    Optional<Category> findBySlugIgnoreCase(String slug);

    Optional<Category> findBySlugIgnoreCaseAndActiveTrue(String slug);

    List<Category> findAllByActiveTrueOrderBySortOrderAscNameAsc();
}
