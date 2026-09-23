package in.sapphirus.rupee.auth.event;

import in.sapphirus.rupee.auth.domain.AuditLog;
import in.sapphirus.rupee.auth.repo.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AuditSecurityListener {

    private static final Logger log = LoggerFactory.getLogger(AuditSecurityListener.class);

    private final AuditLogRepository auditLogRepository;

    public AuditSecurityListener(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    @EventListener
    public void handleAuditSecurityEvent(AuditSecurityEvent event) {
        log.info("Processing asynchronous audit log security event: action={}, result={}, user={}", 
                event.action(), event.result(), event.userId());
        try {
            AuditLog auditEntry = new AuditLog(
                    event.userId(),
                    event.action(),
                    event.result(),
                    event.failureReason(),
                    event.ipAddress()
            );
            auditLogRepository.save(auditEntry);
            log.info("Audit entry successfully persisted to PostgreSQL auth.audit_log ledger");
        } catch (Exception e) {
            log.error("Failed to persist security audit event asynchronously: {}", e.getMessage());
        }
    }
}
