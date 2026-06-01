package abdellah.ecommerce.service;

import abdellah.ecommerce.domain.entity.AuditLog;
import abdellah.ecommerce.domain.entity.AppUser;
import abdellah.ecommerce.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void log(AppUser actorUser, String actionType, String entityName, Long entityId, Object oldData, Object newData, String ipAddress, String userAgent) {
        AuditLog log = new AuditLog();
        log.setActorUser(actorUser);
        log.setActionType(actionType);
        log.setEntityName(entityName);
        log.setEntityId(entityId);
        log.setOldData(toText(oldData));
        log.setNewData(toText(newData));
        log.setIpAddress(ipAddress);
        log.setUserAgent(userAgent);
        auditLogRepository.save(log);
    }

    private String toText(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
