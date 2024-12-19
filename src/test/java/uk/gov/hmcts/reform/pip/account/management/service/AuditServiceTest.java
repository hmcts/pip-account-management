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
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {
    private static final UUID ID = UUID.randomUUID();

    @Mock
    private AuditRepository auditRepository;

    @InjectMocks
    private AuditService auditService;

    private static final String EMAIL = "a@b.com";
    private static final String USER_ID = "123";
    private static final List<AuditAction> AUDIT_ACTIONS = new ArrayList<>();
    private static final String FILTER_DATE = "2024-11-01";
    private final AuditLog auditLogExample = new AuditLog();

    @BeforeEach
    void setup() {
        auditLogExample.setId(ID);
        auditLogExample.setUserId("1234");
        auditLogExample.setUserEmail("test@justice.gov.uk");
        auditLogExample.setAction(AuditAction.MANAGE_USER);
        auditLogExample.setDetails("Test details for manage user");
        auditLogExample.setTimestamp(LocalDateTime.now());

        AUDIT_ACTIONS.add(AuditAction.ADMIN_CREATION);
    }

    @Test
    void testGetAllAuditLogs() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<AuditLog> page = new PageImpl<>(List.of(auditLogExample), pageable, List.of(auditLogExample).size());
        when(auditRepository
            .findAllByUserEmailLikeIgnoreCaseAndUserIdLikeAndActionInOrderByTimestampDesc(
                "%" + EMAIL + "%", USER_ID, AUDIT_ACTIONS, pageable))
            .thenReturn(page);

        Page<AuditLog> returnedAuditLogs = auditService.getAllAuditLogs(pageable, EMAIL, USER_ID, AUDIT_ACTIONS,
                                                                        "");

        assertEquals(auditLogExample, returnedAuditLogs.getContent().get(0),
                     "Returned audit log does not match the expected");
    }

    @Test
    void testGetAllAuditLogsWithFilterDate() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<AuditLog> page = new PageImpl<>(List.of(auditLogExample), pageable, List.of(auditLogExample).size());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDateTime filterStartDate = LocalDate.parse(FILTER_DATE, formatter).atTime(LocalTime.MIN);
        LocalDateTime filterEndDate = LocalDate.parse(FILTER_DATE, formatter).atTime(LocalTime.MAX);

        when(auditRepository
                 .findAllByUserEmailLikeIgnoreCaseAndUserIdLikeAndActionInAndTimestampBetweenOrderByTimestampDesc(
                     "%" + EMAIL + "%", USER_ID, AUDIT_ACTIONS, filterStartDate, filterEndDate, pageable))
            .thenReturn(page);

        Page<AuditLog> returnedAuditLogs = auditService.getAllAuditLogs(pageable, EMAIL, USER_ID, AUDIT_ACTIONS,
                                                                        FILTER_DATE);

        assertEquals(auditLogExample, returnedAuditLogs.getContent().get(0),
                     "Returned audit log does not match the expected");
    }

    @Test
    void testGetAuditLogById() {
        when(auditRepository.findById(ID)).thenReturn(Optional.of(auditLogExample));

        AuditLog returnedAuditLog = auditService.getAuditLogById(ID);

        assertEquals(auditLogExample, returnedAuditLog,
                     "Returned audit log does not match the expected");
    }

    @Test
    void testGetAuditLogByIdNotFound() {
        when(auditRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auditService.getAuditLogById(ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessage("Audit log with id " + ID + " could not be found");
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

    @Test
    void deleteAuditLogsByEmail() {
        List<AuditLog> auditLogs = List.of(auditLogExample);
        when(auditRepository.findAllByUserEmailStartingWithIgnoreCase(EMAIL)).thenReturn(auditLogs);

        String response = auditService.deleteAllLogsWithUserEmailPrefix(EMAIL);

        assertEquals("1 audit log(s) deleted with user email starting with " + EMAIL, response,
                     "Deletion response was not as expected");
        verify(auditRepository, times(1)).deleteByIdIn(any());
    }
}
