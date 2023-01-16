package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pip.account.management.service.InactiveAccountManagementService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;

@ExtendWith(MockitoExtension.class)
class InactiveAccountManagementControllerTest {
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    private InactiveAccountManagementService inactiveAccountManagementService;

    @InjectMocks
    private InactiveAccountManagementController inactiveAccountManagementController;

    @Test
    void testNotifyInactiveMediaAccounts() {
        doNothing().when(inactiveAccountManagementService).sendMediaUsersForVerification();
        assertThat(inactiveAccountManagementController.notifyInactiveMediaAccounts().getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testDeleteExpiredAccounts() {
        doNothing().when(inactiveAccountManagementService).findMediaAccountsForDeletion();
        assertThat(inactiveAccountManagementController.deleteExpiredMediaAccounts().getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testNotifyInactiveAdminAccounts() {
        doNothing().when(inactiveAccountManagementService).notifyAdminUsersToSignIn();
        assertThat(inactiveAccountManagementController.notifyInactiveAdminAccounts().getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testDeleteExpiredAdminAccounts() {
        doNothing().when(inactiveAccountManagementService).findAdminAccountsForDeletion();
        assertThat(inactiveAccountManagementController.deleteExpiredAdminAccounts().getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testNotifyInactiveIdamAccounts() {
        doNothing().when(inactiveAccountManagementService).notifyIdamUsersToSignIn();
        assertThat(inactiveAccountManagementController.notifyInactiveIdamAccounts().getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testDeleteExpiredIdamAccounts() {
        doNothing().when(inactiveAccountManagementService).findIdamAccountsForDeletion();
        assertThat(inactiveAccountManagementController.deleteExpiredIdamAccounts().getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.NO_CONTENT);
    }
}
