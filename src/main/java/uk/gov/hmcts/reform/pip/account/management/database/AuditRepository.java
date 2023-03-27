package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;

import java.time.LocalDateTime;
import java.util.UUID;

public interface AuditRepository extends JpaRepository<AuditLog, UUID> {

    @Transactional
    void deleteAllByTimestampBefore(LocalDateTime timestamp);

    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);
}
