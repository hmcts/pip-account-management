package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.service.SystemAdminAccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAdminAccountControllerTest {
    private static final String TEST_EMAIL = "test@user.com";
    private static final String TEST_PROVENANCE_ID = "1234";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";
    private static final String RESPONSE_BODY_MATCH = "Expected user does not match";

    @Mock
    private SystemAdminAccountService systemAdminAccountService;

    @InjectMocks
    private SystemAdminAccountController systemAdminAccountController;

    @Test
    void testCreateSystemAdminAccount() {
        PiUser expectedUser = new PiUser();
        expectedUser.setEmail(TEST_EMAIL);
        expectedUser.setRoles(Roles.SYSTEM_ADMIN);
        expectedUser.setUserProvenance(UserProvenances.SSO);
        expectedUser.setProvenanceUserId(TEST_PROVENANCE_ID);

        SystemAdminAccount testAccount = new SystemAdminAccount(TEST_EMAIL, "Test", "User", TEST_PROVENANCE_ID);

        String testIssuerId = "1234";
        when(systemAdminAccountService.addSystemAdminAccount(testAccount)).thenReturn(expectedUser);

        ResponseEntity<? extends PiUser> response = systemAdminAccountController.createSystemAdminAccount(testAccount);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(expectedUser, response.getBody(), RESPONSE_BODY_MATCH);
    }
}
