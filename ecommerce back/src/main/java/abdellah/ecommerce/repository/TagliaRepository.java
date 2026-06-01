package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.Taglia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TagliaRepository extends JpaRepository<Taglia, Long> {

    List<Taglia> findAllByOrderByNomeAscCodiceAsc();

    Optional<Taglia> findByCodiceIgnoreCase(String codice);

    boolean existsByCodiceIgnoreCase(String codice);

    boolean existsByCodiceIgnoreCaseAndIdNot(String codice, Long id);
}
