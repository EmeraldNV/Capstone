package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    boolean existsByEmailIgnoreCase(String email);

    Optional<AppUser> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<AppUser> findWithRolesByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role"})
    Optional<AppUser> findWithRolesById(Long id);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "customerProfile"})
    List<AppUser> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "customerProfile"})
    List<AppUser> findByEmailContainingIgnoreCaseOrderByCreatedAtDesc(String email);

    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "customerProfile"})
    Page<AppUser> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
