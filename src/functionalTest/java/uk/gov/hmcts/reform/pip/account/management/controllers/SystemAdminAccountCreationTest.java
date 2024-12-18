package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
class SystemAdminAccountCreationTest extends FunctionalTestBase {
    private static final String TEST_USER_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_USER_EMAIL = TEST_USER_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String TEST_USER_PROVENANCE_ID = UUID.randomUUID().toString();

    private static final String TESTING_SUPPORT_ACCOUNT_URL = "/testing-support/account/";
    private static final String ACCOUNT_URL = "/account";
    private static final String SYSTEM_ADMIN_URL = ACCOUNT_URL + "/system-admin";
    private static final String BEARER = "Bearer ";

    private Map<String, String> bearer;

    @BeforeAll
    public void startUp() {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_USER_EMAIL, bearer);
    }

    @Test
    void createSystemAdminAccount() {
        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_USER_EMAIL, TEST_USER_PROVENANCE_ID);

        Response response = doPostRequest(SYSTEM_ADMIN_URL, bearer, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("email")).isEqualTo(TEST_USER_EMAIL);
        assertThat(response.jsonPath().getString("provenanceUserId")).isEqualTo(TEST_USER_PROVENANCE_ID);
    }

    @Test
    void shouldFailToCreateSystemAdminAccountWithoutEmail() {
        String requestBody = """
            {
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_USER_PROVENANCE_ID);

        Response response = doPostRequest(SYSTEM_ADMIN_URL, bearer, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldFailToCreateSystemAdminAccountWithoutProvenanceUserId() {
        String requestBody = """
            {
                "email": "%s"
            }
            """.formatted(TEST_USER_EMAIL);

        Response response = doPostRequest(SYSTEM_ADMIN_URL, bearer, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

}
