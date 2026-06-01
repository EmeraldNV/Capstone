package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    boolean existsByUser_IdAndRole_Id(Long userId, Long roleId);

    List<UserRole> findByUser_Id(Long userId);
}
