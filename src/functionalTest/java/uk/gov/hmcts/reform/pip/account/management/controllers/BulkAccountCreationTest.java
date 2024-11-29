package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;

public class BulkAccountCreationTest extends FunctionalTestBase {
    private static final String ISSUER_ID = "x-issuer-id";
    private static final String MOCK_FILE = "files/test-csv.csv";
    private static final String BULK_UPLOAD_URL = "account/media-bulk-upload";
    private static final String BEARER = "Bearer ";
    private static final String TESTING_SUPPORT_APPLICATION_URL = "/testing-support/application/";
    private static final String TEST_EMAIL = "pip-bulk-test-email@test.com";

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_APPLICATION_URL + TEST_EMAIL,
                        Map.of(AUTHORIZATION, BEARER + accessToken), "");
    }

    @Test
    void shouldBeAbleToCreateAccountsInBulk() throws Exception {

        final Response response =
            doPostMultipartForBulkUpload(BULK_UPLOAD_URL, Map.of(AUTHORIZATION, BEARER + accessToken),
                                          new ClassPathResource(MOCK_FILE).getFile(), ISSUER_ID);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
    }
}
