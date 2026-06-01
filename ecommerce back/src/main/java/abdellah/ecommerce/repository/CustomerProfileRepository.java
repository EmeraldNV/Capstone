package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.CustomerProfile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    @EntityGraph(attributePaths = {"user", "addresses"})
    Optional<CustomerProfile> findWithDetailsByUser_Id(Long userId);

    Optional<CustomerProfile> findByUser_Id(Long userId);
}
