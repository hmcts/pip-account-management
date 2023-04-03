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
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    private SystemAdminAccountService systemAdminAccountService;

    @InjectMocks
    private SystemAdminAccountController systemAdminAccountController;

    @Test
    void testCreateSystemAdminAccount() {
        PiUser testUser = new PiUser();
        testUser.setEmail(TEST_EMAIL);
        testUser.setRoles(Roles.SYSTEM_ADMIN);
        testUser.setUserProvenance(UserProvenances.PI_AAD);

        SystemAdminAccount testAccount = new SystemAdminAccount(TEST_EMAIL,
                                                                "Test", "User");

        String testIssuerId = "1234";
        when(systemAdminAccountService.addSystemAdminAccount(testAccount, testIssuerId)).thenReturn(testUser);

        ResponseEntity<? extends PiUser> response = systemAdminAccountController.createSystemAdminAccount(
            testIssuerId, testAccount
        );

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(testUser, response.getBody(), "Should return the created piUser");
    }
}
