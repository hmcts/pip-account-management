package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.AuditRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Service layer that deals with auditing operations.
 */
@Slf4j
@Service
@AllArgsConstructor
public class AuditService {
    private static final String AUDIT_LOG_NOT_FOUND = "Audit log with id %s could not be found";

    private final AuditRepository auditRepository;

    /**
     * Get all audit logs in a page object and descending order on timestamp.
     *
     * @param pageable The pageable object to query by.
     * @return Returns the audit logs in a page.
     */
    public Page<AuditLog> getAllAuditLogs(Pageable pageable, String email, String userId,
                                          List<AuditAction> auditActions, String filterDate) {

        // If user id is supplied then find by an exact match
        String userIdToQuery = "%%";
        if (!userId.isBlank()) {
            userIdToQuery = userId;
        }

        // If user audit action is supplied then find by an exact match
        List<AuditAction> auditActionsToQuery = new ArrayList<>(EnumSet.allOf(AuditAction.class));
        if (!auditActions.isEmpty()) {
            auditActionsToQuery = auditActions;
        }

        if (filterDate.isBlank()) {
            return auditRepository
                .findAllByUserEmailLikeIgnoreCaseAndUserIdLikeAndActionInOrderByTimestampDesc(
                    "%" + email + "%",
                    userIdToQuery,
                    auditActionsToQuery,
                    pageable);
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime filterStartDate = LocalDate.parse(filterDate, formatter).atTime(LocalTime.MIN);
        LocalDateTime filterEndDate = LocalDate.parse(filterDate, formatter).atTime(LocalTime.MAX);

        return auditRepository
            .findAllByUserEmailLikeIgnoreCaseAndUserIdLikeAndActionInAndTimestampBetweenOrderByTimestampDesc(
                "%" + email + "%",
                userIdToQuery,
                auditActionsToQuery,
                filterStartDate,
                filterEndDate,
                pageable);
    }

    public AuditLog getAuditLogById(UUID id) {
        return auditRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(String.format(AUDIT_LOG_NOT_FOUND, id)));
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

    public String deleteAllLogsWithUserEmailPrefix(String prefix) {
        List<AuditLog> auditLogsToDelete = auditRepository
            .findAllByUserEmailStartingWithIgnoreCase(prefix);

        if (!auditLogsToDelete.isEmpty()) {

            List<UUID> auditLogIds = auditLogsToDelete.stream()
                .map(AuditLog::getId)
                .toList();
            auditRepository.deleteByIdIn(auditLogIds);
        }
        return String.format("%s audit log(s) deleted with user email starting with %s",
                             auditLogsToDelete.size(), prefix);
    }
}
