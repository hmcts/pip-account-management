package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuditRepositoryTest {
    private static final String USER_ID1 = "123";
    private static final String USER_ID2 = "124";
    private static final String USER_ID3 = "125";
    private static final String EMAIL1 = "TestUser1@justice.gov.uk";
    private static final String EMAIL2 = "TestUser2@justice.gov.uk";
    private static final String EMAIL3 = "TestUser3@justice.gov.uk";
    private static final String DETAILS1 = "Details 1";
    private static final String DETAILS2 = "Details 2";
    private static final String DETAILS3 = "Details 3";

    private static final LocalDateTime TIMESTAMP_NOW = LocalDateTime.now();
    private static final String AUDIT_LOG_MATCHED_MESSAGE = "Audit log does not match";

    @Autowired
    AuditRepository auditRepository;

    @BeforeEach
    void setup() {
        AuditLog auditLog1 = new AuditLog();
        auditLog1.setUserId(USER_ID1);
        auditLog1.setUserEmail(EMAIL1);
        auditLog1.setRoles(Roles.SYSTEM_ADMIN);
        auditLog1.setUserProvenance(UserProvenances.SSO);
        auditLog1.setAction(AuditAction.MANAGE_USER);
        auditLog1.setDetails(DETAILS1);
        auditLog1.setTimestamp(TIMESTAMP_NOW);

        AuditLog auditLog2 = new AuditLog();
        auditLog2.setUserId(USER_ID2);
        auditLog2.setUserEmail(EMAIL2);
        auditLog2.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        auditLog2.setUserProvenance(UserProvenances.SSO);
        auditLog2.setAction(AuditAction.REFERENCE_DATA_UPLOAD);
        auditLog2.setDetails(DETAILS2);
        auditLog2.setTimestamp(TIMESTAMP_NOW.minusHours(1));

        AuditLog auditLog3 = new AuditLog();
        auditLog3.setUserId(USER_ID3);
        auditLog3.setUserEmail(EMAIL3);
        auditLog3.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        auditLog3.setUserProvenance(UserProvenances.SSO);
        auditLog3.setAction(AuditAction.DELETE_PUBLICATION);
        auditLog3.setDetails(DETAILS3);
        auditLog3.setTimestamp(TIMESTAMP_NOW.plusHours(1));

        auditRepository.saveAll(List.of(auditLog1, auditLog2, auditLog3));
    }

    @AfterEach
    void shutdown() {
        auditRepository.deleteAll();
    }

    @Test
    void shouldDeleteAllByTimestampBefore() {
        auditRepository.deleteAllByTimestampBefore(TIMESTAMP_NOW.plusHours(1));

        assertThat(auditRepository.findAll())
            .as(AUDIT_LOG_MATCHED_MESSAGE)
            .hasSize(1)
            .extracting(AuditLog::getUserId)
            .containsExactly(USER_ID3);
    }

    @Test
    void shouldNotDeleteAllByTimestampBefore() {
        auditRepository.deleteAllByTimestampBefore(TIMESTAMP_NOW.minusHours(1));

        assertThat(auditRepository.findAll())
            .as(AUDIT_LOG_MATCHED_MESSAGE)
            .hasSize(3);
    }
}
