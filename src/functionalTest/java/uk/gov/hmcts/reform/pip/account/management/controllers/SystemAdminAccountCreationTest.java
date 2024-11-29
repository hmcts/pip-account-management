package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
public class SystemAdminAccountCreationTest extends FunctionalTestBase {
    private static final String TEST_EMAIL_PREFIX = String.format(
            "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_EMAIL = String.format("%s@justice.gov.uk", TEST_EMAIL_PREFIX);
    private static final String TEST_PROVENANCE_ID = "1234";
    private static final String TEST_FIRST_NAME = "E2E Account Management Test First Name";
    private static final String TEST_SURNAME = "E2E Account Management Test Surname";
    private static final String ACCOUNT_URL = "/account";
    private static final String SYSTEM_ADMIN_URL = ACCOUNT_URL + "/system-admin";
    private static final String BEARER = "Bearer ";


    @Test
    void shouldBeAbleToCreateASystemAdminAccount() {
        SystemAdminAccount requestBody =
                new SystemAdminAccount(TEST_EMAIL, TEST_FIRST_NAME, TEST_SURNAME, TEST_PROVENANCE_ID);

        final Response response =
                doPostRequest(SYSTEM_ADMIN_URL, Map.of(AUTHORIZATION, BEARER + accessToken), requestBody.toString());

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
    }
}
