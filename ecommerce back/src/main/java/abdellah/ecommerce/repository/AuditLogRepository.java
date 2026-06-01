package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.AuditLog;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @EntityGraph(attributePaths = {"actorUser"})
    List<AuditLog> findTop100ByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"actorUser"})
    List<AuditLog> findTop100ByEntityNameOrderByCreatedAtDesc(String entityName);
}
