package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.account.management.service.AuditService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuditController auditController;

    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Test
    void testGetAllAuditLogs() {
        ResponseEntity<Page<AuditLog>> response = auditController.getAllAuditLogs(0, 25);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testGetAuditLogById() {
        UUID id = UUID.randomUUID();
        when(auditService.getAuditLogById(id)).thenReturn(new AuditLog());

        ResponseEntity<AuditLog> response = auditController.getAuditLogById(id);
        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testCreateAuditLog() {
        AuditLog auditLog = new AuditLog();

        auditLog.setUserId("1234");
        auditLog.setUserEmail("test@justice.gov.uk");
        auditLog.setRoles(Roles.SYSTEM_ADMIN);
        auditLog.setUserProvenance(UserProvenances.PI_AAD);
        auditLog.setAction(AuditAction.DELETE_USER);
        auditLog.setDetails("Manage user test");

        when(auditService.createAuditLog(auditLog)).thenReturn(auditLog);

        ResponseEntity<AuditLog> response = auditController.createAuditLog(auditLog);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(auditLog, response.getBody(), "Returned audit log model does not match expected");
    }

    @Test
    void testDeleteAuditLog() {
        when(auditService.deleteAuditLogs())
            .thenReturn("Audit logs that met the max retention period have been deleted");
        ResponseEntity<String> response = auditController.deleteAuditLogs();

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals("Audit logs that met the max retention period have been deleted",
                     response.getBody(), "Should return expected deletion message"
        );
    }
}
