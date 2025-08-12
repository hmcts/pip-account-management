package uk.gov.hmcts.reform.pip.account.management;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class SystemAdminAccountCreationTest extends AccountHelperBase {
    private static final String TEST_USER_EMAIL = TEST_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String TEST_USER_PROVENANCE_ID = UUID.randomUUID().toString();

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_USER_EMAIL, bearer);
    }

    @Test
    void testCreateSystemAdminAccount() {
        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_USER_EMAIL, TEST_USER_PROVENANCE_ID);

        Response response = doPostRequest(CREATE_SYSTEM_ADMIN_SSO, bearer, requestBody);

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

        Response response = doPostRequest(CREATE_SYSTEM_ADMIN_SSO, bearer, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void shouldFailToCreateSystemAdminAccountWithoutProvenanceUserId() {
        String requestBody = """
            {
                "email": "%s"
            }
            """.formatted(TEST_USER_EMAIL);

        Response response = doPostRequest(CREATE_SYSTEM_ADMIN_SSO, bearer, requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
    }

}
