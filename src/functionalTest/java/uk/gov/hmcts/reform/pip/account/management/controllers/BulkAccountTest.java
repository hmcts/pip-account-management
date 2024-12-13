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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
class BulkAccountTest extends FunctionalTestBase {
    private static final String USER_ID = UUID.randomUUID().toString();
    private static final String COMMON_PREFIX = "pip-am-test-email-bulk-";
    private static final String MOCK_FILE = "files/test-csv.csv";
    private static final String BULK_UPLOAD_URL = "account/media-bulk-upload";
    private static final String TESTING_SUPPORT_ACCOUNT_URL = "/testing-support/account/";
    private static final String BEARER = "Bearer ";
    private static final String ISSUER_ID = "x-issuer-id";

    private Map<String, String> bearer;
    private Map<String, String> issuerId;

    @BeforeAll
    void startUp() throws IOException {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
        issuerId = Map.of(ISSUER_ID, USER_ID);

        StringBuilder csvContent = new StringBuilder(400);
        csvContent.append("email,firstName,surname\n");

        for (int i = 0; i < 5; i++) {
            String email = generateTestEmail();
            csvContent.append(email).append(",testBulkFirstName,testBulkSurname\n");
        }

        Path mockFilePath = Files.createTempFile("mock-bulk-upload", ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(mockFilePath)) {
            writer.write(csvContent.toString());
        }

        System.setProperty(MOCK_FILE, mockFilePath.toString());
    }

    private String generateTestEmail() {
        String prefix = String.format(COMMON_PREFIX + "%s", ThreadLocalRandom.current().nextInt(1000, 9999));
        return prefix + "@justice.gov.uk";
    }

    @AfterAll
    public void teardown() throws IOException {
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + COMMON_PREFIX, bearer);
        Files.deleteIfExists(Path.of(MOCK_FILE));
    }

    @Test
    void createAccountsInBulk() {
        File mockFile = new File(System.getProperty(MOCK_FILE));

        Response response = doPostMultipartForBulk(BULK_UPLOAD_URL, bearer, issuerId, mockFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("CREATED_ACCOUNTS").size()).isEqualTo(5);
        assertThat(response.jsonPath().getList("ERRORED_ACCOUNTS").isEmpty()).isTrue();
    }

    @Test
    void shouldReturnOkButNotCreateAccountsForDuplicates() {
        File mockFile = new File(System.getProperty(MOCK_FILE));

        Response response = doPostMultipartForBulk(BULK_UPLOAD_URL, bearer, issuerId, mockFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("CREATED_ACCOUNTS").isEmpty()).isTrue();
        assertThat(response.jsonPath().getList("ERRORED_ACCOUNTS").isEmpty()).isTrue();
    }
}
