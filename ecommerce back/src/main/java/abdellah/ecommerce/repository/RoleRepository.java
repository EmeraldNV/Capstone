package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByRoleCodeIgnoreCase(String roleCode);

    List<Role> findByRoleCodeIn(Collection<String> roleCodes);
}
