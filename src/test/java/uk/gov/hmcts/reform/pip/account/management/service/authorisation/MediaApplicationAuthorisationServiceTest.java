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
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.UnitTestAssertionsShouldIncludeMessage")
class MediaApplicationAuthorisationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String INTERNAL_ADMIN_CTSC_ROLE = "INTERNAL_ADMIN_CTSC";

    private static PiUser user = new PiUser();
    private static PiUser adminUser = new PiUser();

    @Mock
    private AccountService accountService;

    @Mock
    private AuthorisationCommonService authorisationCommonService;

    @InjectMocks
    private MediaApplicationAuthorisationService mediaApplicationAuthorisationService;

    @BeforeEach
    void beforeEachSetup() {
        when(authorisationCommonService.isAdmin()).thenReturn(true);
    }

    @BeforeAll
    static void setup() {
        user.setUserId(USER_ID);
        adminUser.setUserId(ADMIN_USER_ID);
    }

    @Test
    void testSystemAdminUserCanBulkCreateMediaAccounts() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        assertTrue(mediaApplicationAuthorisationService.userCanBulkCreateMediaAccounts(ADMIN_USER_ID));
    }

    @Test
    void testSystemAdminUserCanNotBulkCreateMediaAccountsWhenNotLoggedIn() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.isAdmin()).thenReturn(false);

        assertFalse(mediaApplicationAuthorisationService.userCanBulkCreateMediaAccounts(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { SYSTEM_ADMIN_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotBulkCreateMediaAccountsWhenNotSystemAdmin(Roles role) {
        user.setRoles(role);

        assertFalse(mediaApplicationAuthorisationService.userCanBulkCreateMediaAccounts(USER_ID));
    }

    @Test
    void testAdminCtscUserCanViewMediaApplications() {
        adminUser.setRoles(INTERNAL_ADMIN_CTSC);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);
        when(authorisationCommonService.isAdmin()).thenReturn(true);

        assertTrue(mediaApplicationAuthorisationService.userCanViewMediaApplications(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { INTERNAL_ADMIN_CTSC_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotViewMediaApplicationsWhenNotAdminCtsc(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        assertFalse(mediaApplicationAuthorisationService.userCanViewMediaApplications(USER_ID));
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testUserCanNotViewMediaApplicationsWhenNotLoggedIn(Roles role) {
        user.setRoles(role);
        when(authorisationCommonService.isAdmin()).thenReturn(false);

        assertFalse(mediaApplicationAuthorisationService.userCanViewMediaApplications(USER_ID));
    }

    @Test
    void testAdminCtscUserCanUpdateMediaApplications() {
        adminUser.setRoles(INTERNAL_ADMIN_CTSC);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);
        when(authorisationCommonService.isAdmin()).thenReturn(true);

        assertTrue(mediaApplicationAuthorisationService.userCanUpdateMediaApplications(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { INTERNAL_ADMIN_CTSC_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotUpdateMediaApplicationsWhenNotAdminCtsc(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        assertFalse(mediaApplicationAuthorisationService.userCanUpdateMediaApplications(USER_ID));
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testUserCanNotUpdateMediaApplicationsWhenNotLoggedIn(Roles role) {
        user.setRoles(role);
        when(authorisationCommonService.isAdmin()).thenReturn(false);

        assertFalse(mediaApplicationAuthorisationService.userCanUpdateMediaApplications(USER_ID));
    }
}
