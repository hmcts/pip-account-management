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
class SystemAdminB2CAccountCreationTest extends FunctionalTestBase {
    private static final String TEST_USER_EMAIL_PREFIX_1 = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_USER_EMAIL_PREFIX_2 = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_USER_EMAIL_PREFIX_3 = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_USER_EMAIL_1 = TEST_USER_EMAIL_PREFIX_1 + "@justice.gov.uk";
    private static final String TEST_USER_EMAIL_2 = TEST_USER_EMAIL_PREFIX_2 + "@justice.gov.uk";
    private static final String TEST_USER_EMAIL_3 = TEST_USER_EMAIL_PREFIX_3 + "@justice.gov.uk";
    private static final String TEST_USER_PROVENANCE_ID = UUID.randomUUID().toString();

    private static final String TESTING_SUPPORT_ACCOUNT_URL = "/testing-support/account/";
    private static final String ACCOUNT_URL = "/account";
    private static final String SYSTEM_ADMIN_B2C_URL = ACCOUNT_URL + "/add/system-admin";
    private static final String SYSTEM_ADMIN_SSO_URL = ACCOUNT_URL + "/system-admin";
    private static final String BEARER = "Bearer ";
    private static final String ISSUER_ID = "x-issuer-id";

    private Map<String, String> bearer;
    private Map<String, String> issuerId;

    @BeforeAll
    public void startUp() {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_USER_EMAIL_3, UUID.randomUUID().toString());

        String userId =  doPostRequest(SYSTEM_ADMIN_SSO_URL, bearer, requestBody)
            .jsonPath().getString("userId");

        issuerId = Map.of(ISSUER_ID, userId);
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_USER_EMAIL_1, bearer);
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_USER_EMAIL_2, bearer);
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_USER_EMAIL_3, bearer);
    }

    @Test
    void createSystemAdminB2CAccount() {
        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_USER_EMAIL_1, TEST_USER_PROVENANCE_ID);

        Response response = doPostRequestForB2C(SYSTEM_ADMIN_B2C_URL, bearer, issuerId, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("email")).isEqualTo(TEST_USER_EMAIL_1);
        assertThat(response.jsonPath().getString("provenanceUserId")).isNotNull();
    }

    @Test
    void shouldCreateSystemAdminB2CAccountWithoutProvenanceUserId() {
        String requestBody = """
            {
                "email": "%s"
            }
            """.formatted(TEST_USER_EMAIL_2);

        Response response = doPostRequestForB2C(SYSTEM_ADMIN_B2C_URL, bearer, issuerId, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("email")).isEqualTo(TEST_USER_EMAIL_2);
        assertThat(response.jsonPath().getString("provenanceUserId")).isNotNull();
    }

    @Test
    void shouldFailToCreateSystemAdminB2CAccountWithoutEmail() {
        String requestBody = """
            {
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_USER_PROVENANCE_ID);

        Response response = doPostRequestForB2C(SYSTEM_ADMIN_B2C_URL, bearer, issuerId, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldFailToCreateSystemAdminB2CAccountWithoutIssuerId() {
        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_USER_EMAIL_1, TEST_USER_PROVENANCE_ID);

        Response response = doPostRequest(SYSTEM_ADMIN_B2C_URL, bearer, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
