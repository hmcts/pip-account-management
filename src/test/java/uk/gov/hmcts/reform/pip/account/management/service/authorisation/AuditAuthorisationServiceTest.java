package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestAssertionsShouldIncludeMessage")
class AuditAuthorisationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";

    private static PiUser user = new PiUser();
    private static PiUser adminUser = new PiUser();

    @Mock
    private AuthorisationCommonService authorisationCommonService;

    @InjectMocks
    private AuditAuthorisationService auditAuthorisationService;

    @BeforeEach
    void beforeEachSetup() {
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);
    }

    @BeforeAll
    static void setup() {
        user.setUserId(USER_ID);
        adminUser.setUserId(ADMIN_USER_ID);
    }

    @Test
    void testSystemAdminUserCanViewAuditLogs() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        assertTrue(auditAuthorisationService.userCanViewAuditLogs(ADMIN_USER_ID));
    }

    @Test
    void testSystemAdminUserCanNotViewAuditLogsWhenNotLoggedIn() {
        adminUser.setRoles(SYSTEM_ADMIN);

        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertFalse(auditAuthorisationService.userCanViewAuditLogs(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { SYSTEM_ADMIN_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotViewAuditLogsWhenNotSystemAdmin(Roles role) {
        user.setRoles(role);

        assertFalse(auditAuthorisationService.userCanViewAuditLogs(USER_ID));
    }
}
