package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uk.gov.hmcts.reform.pip.account.management.database.AuditRepository;
import uk.gov.hmcts.reform.pip.account.management.model.AuditAction;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditRepository auditRepository;

    @InjectMocks
    private AuditService auditService;

    private final AuditLog auditLogExample = new AuditLog();

    @BeforeEach
    void setup() {
        auditLogExample.setId(UUID.randomUUID());
        auditLogExample.setUserId("1234");
        auditLogExample.setUserEmail("test@justice.gov.uk");
        auditLogExample.setAction(AuditAction.MANAGE_USER);
        auditLogExample.setDetails("Test details for manage user");
        auditLogExample.setTimestamp(LocalDateTime.now());
    }

    @Test
    void testGetAllAuditLogs() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<AuditLog> page = new PageImpl<>(List.of(auditLogExample), pageable, List.of(auditLogExample).size());
        when(auditRepository.findAllByOrderByTimestampDesc(pageable)).thenReturn(page);

        Page<AuditLog> returnedAuditLogs = auditService.getAllAuditLogs(pageable);

        assertEquals(auditLogExample, returnedAuditLogs.getContent().get(0),
                     "Returned audit log does not match the expected");
    }

    @Test
    void testCreateAuditLog() {
        when(auditRepository.save(auditLogExample)).thenReturn(auditLogExample);

        AuditLog createdAuditLog = auditService.createAuditLog(auditLogExample);
        assertEquals(auditLogExample, createdAuditLog, "Created audit log does not match expected");
    }

    @Test
    void deleteAuditLogs() {
        doNothing().when(auditRepository).deleteAllByTimestampBefore(any());

        String response = auditService.deleteAuditLogs();

        assertEquals("Audit logs that met the max retention period have been deleted",
                     response, "Deletion response was not as expected");
        verify(auditRepository, times(1)).deleteAllByTimestampBefore(any());
    }
}
