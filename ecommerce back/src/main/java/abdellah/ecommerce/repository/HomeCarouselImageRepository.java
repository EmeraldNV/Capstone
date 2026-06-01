package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.HomeCarouselImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeCarouselImageRepository extends JpaRepository<HomeCarouselImage, Long> {

    List<HomeCarouselImage> findAllByActiveTrueOrderBySortOrderAscCreatedAtAsc();

    List<HomeCarouselImage> findAllByOrderBySortOrderAscCreatedAtAsc();
}
