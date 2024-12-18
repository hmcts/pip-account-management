package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SystemAdminB2CAccountCreationTest extends AccountHelperBase {
    private static final String ACCOUNT_URL = "/account";
    private static final String SYSTEM_ADMIN_B2C_URL = ACCOUNT_URL + "/add/system-admin";

    private Map<String, String> issuerId;
    private String testEmail;
    private String testProvenanceUserId;

    @BeforeAll
    public void startUp() throws JsonProcessingException {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        PiUser piUser = createSystemAdminAccount();
        issuerId = Map.of(ISSUER_ID, piUser.getUserId());
    }

    @BeforeEach
    public void setupTest() {
        testEmail = generateEmail();
        testProvenanceUserId = UUID.randomUUID().toString();
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, bearer);
    }

    @Test
    void createSystemAdminB2CAccount() {

        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(testEmail, testProvenanceUserId);

        Response response = doPostRequestForB2C(SYSTEM_ADMIN_B2C_URL, bearer, issuerId, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("email")).isEqualTo(testEmail);
        assertThat(response.jsonPath().getString("provenanceUserId")).isNotNull();
    }

    @Test
    void shouldCreateSystemAdminB2CAccountWithoutProvenanceUserId() {
        String requestBody = """
            {
                "email": "%s"
            }
            """.formatted(testEmail);

        Response response = doPostRequestForB2C(SYSTEM_ADMIN_B2C_URL, bearer, issuerId, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getString("email")).isEqualTo(testEmail);
        assertThat(response.jsonPath().getString("provenanceUserId")).isNotNull();
    }

    @Test
    void shouldFailToCreateSystemAdminB2CAccountWithoutEmail() {
        String requestBody = """
            {
                "provenanceUserId": "%s"
            }
            """.formatted(testProvenanceUserId);

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
            """.formatted(testEmail, testProvenanceUserId);

        Response response = doPostRequest(SYSTEM_ADMIN_B2C_URL, bearer, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }
}
