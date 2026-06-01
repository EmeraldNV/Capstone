package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.Brand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandRepository extends JpaRepository<Brand, Long> {
}
