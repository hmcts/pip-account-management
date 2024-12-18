package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditLog, UUID> {

    @Transactional
    void deleteAllByTimestampBefore(LocalDateTime timestamp);

    Page<AuditLog> findAllByUserEmailLikeIgnoreCaseAndUserIdLikeAndActionInOrderByTimestampDesc(
        String email,
        String userId,
        List<AuditAction> auditAction,
        Pageable pageable);

    Page<AuditLog> findAllByUserEmailLikeIgnoreCaseAndUserIdLikeAndActionInAndTimestampBetweenOrderByTimestampDesc(
        String email,
        String userId,
        List<AuditAction> auditAction,
        LocalDateTime timeStampFrom,
        LocalDateTime timeStampTo,
        Pageable pageable);
}
