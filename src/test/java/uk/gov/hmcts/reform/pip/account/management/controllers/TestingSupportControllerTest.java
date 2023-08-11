package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;
import uk.gov.hmcts.reform.pip.account.management.service.MediaApplicationService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestingSupportControllerTest {
    private static final String EMAIL_PREFIX = "TEST_PIP_1234_";
    private static final String EMAIL = "test@test.com";
    private static final String ISSUER_ID = "TESTING-SUPPORT";
    private static final String MESSAGE = "Failed to create user";

    private static final String RESPONSE_STATUS_MESSAGE = "Response status does not match";
    private static final String RESPONSE_BODY_MESSAGE = "Response body does not match";

    @Mock
    private AccountService accountService;

    @Mock
    private MediaApplicationService mediaApplicationService;

    @InjectMocks
    TestingSupportController testingSupportController;

    @Test
    void testCreateAccountReturnsCreated() {
        AzureAccount account = new AzureAccount();
        PiUser user = new PiUser();
        user.setEmail(EMAIL);

        when(accountService.addUserWithSuppliedPassword(account, ISSUER_ID))
            .thenReturn(Pair.of(CreationEnum.CREATED_ACCOUNTS, user));
        ResponseEntity response = testingSupportController.createAccount(account);

        assertThat(response.getStatusCode())
            .as(RESPONSE_STATUS_MESSAGE)
            .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
            .as(RESPONSE_BODY_MESSAGE)
            .isInstanceOf(PiUser.class);

        PiUser returnedUser = (PiUser) response.getBody();
        assertThat(returnedUser.getEmail())
            .as(RESPONSE_BODY_MESSAGE)
            .isEqualTo(EMAIL);
    }

    @Test
    void testCreateAccountReturnsBadRequest() {
        AzureAccount account = new AzureAccount();
        when(accountService.addUserWithSuppliedPassword(account, ISSUER_ID))
            .thenReturn(Pair.of(CreationEnum.ERRORED_ACCOUNTS, MESSAGE));
        ResponseEntity response = testingSupportController.createAccount(account);

        assertThat(response.getStatusCode())
            .as(RESPONSE_STATUS_MESSAGE)
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(response.getBody())
            .as(RESPONSE_BODY_MESSAGE)
            .isInstanceOf(String.class);

        assertThat((String)response.getBody())
            .as(RESPONSE_BODY_MESSAGE)
            .isEqualTo(MESSAGE);
    }

    @Test
    void testDeleteAccountsWithEmailPrefixReturnsOk() {
        String responseMessage = "2 account(s) deleted with email starting with " + EMAIL_PREFIX;
        when(accountService.deleteAllAccountsWithEmailPrefix(EMAIL_PREFIX)).thenReturn(responseMessage);

        ResponseEntity<String> response = testingSupportController.deleteAccountsWithEmailPrefix(EMAIL_PREFIX);

        assertThat(response.getStatusCode())
            .as(RESPONSE_STATUS_MESSAGE)
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as(RESPONSE_BODY_MESSAGE)
            .isEqualTo(responseMessage);
    }

    @Test
    void testDeleteMediaApplicationsWithEmailPrefixReturnsOk() {
        String responseMessage = "3 media application(s) deleted with email starting with " + EMAIL_PREFIX;
        when(mediaApplicationService.deleteAllApplicationsWithEmailPrefix(EMAIL_PREFIX)).thenReturn(responseMessage);

        ResponseEntity<String> response = testingSupportController.deleteMediaApplicationsWithEmailPrefix(EMAIL_PREFIX);

        assertThat(response.getStatusCode())
            .as(RESPONSE_STATUS_MESSAGE)
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as(RESPONSE_BODY_MESSAGE)
            .isEqualTo(responseMessage);
    }
}
