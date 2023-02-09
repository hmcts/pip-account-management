package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.AuditRepository;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;

import java.time.LocalDateTime;

/**
 * Service layer that deals with auditing operations.
 */
@Slf4j
@Service
public class AuditService {

    private final AuditRepository auditRepository;

    @Autowired
    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    /**
     * Get all audit logs in a page object and descending order on timestamp.
     *
     * @param pageable The pageable object to query by.
     * @return Returns the audit logs in a page.
     */
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditRepository.findAllByOrderByTimestampDesc(pageable);
    }

    /**
     * Store a new audit log entry.
     *
     * @param auditLog The audit log to save.
     * @return The stored audit log.
     */
    public AuditLog createAuditLog(AuditLog auditLog) {
        return auditRepository.save(auditLog);
    }

    /**
     * Delete all audit log records that have met the max retention period of 90 days.
     *
     * @return A string confirming the deletion.
     */
    public String deleteAuditLogs() {
        auditRepository.deleteAllByTimestampBefore(LocalDateTime.now().minusDays(90));
        return "Audit logs that met the max retention period have been deleted";
    }
}
