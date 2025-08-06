package uk.gov.hmcts.reform.pip.account.management;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class BulkAccountTest extends AccountHelperBase {
    private String mockFile;
    private final Map<String, String> issuerId = Map.of(ISSUER_ID, UUID.randomUUID().toString());

    @BeforeAll
    void startUp() throws IOException {
        StringBuilder csvContent = new StringBuilder(400);
        csvContent.append("email,firstName,surname\n");

        for (int i = 0; i < 5; i++) {
            csvContent.append(generateEmail()).append(",testBulkFirstName,testBulkSurname\n");
        }

        Path mockFilePath = Files.createTempFile("mock-bulk-upload", ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(mockFilePath)) {
            writer.write(csvContent.toString());
        }

        mockFile = mockFilePath.toString();
    }

    @AfterAll
    public void teardown() throws IOException {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, bearer);
        Files.deleteIfExists(Path.of(mockFile));
    }

    @Test
    void createAccountsInBulk() {
        File mockBulkUploadFile = new File(mockFile);

        Response response = doPostMultipartForBulk(BULK_UPLOAD_URL, bearer, issuerId, mockBulkUploadFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("CREATED_ACCOUNTS").size()).isEqualTo(5);
        assertThat(response.jsonPath().getList("ERRORED_ACCOUNTS").isEmpty()).isTrue();
    }

    @Test
    void shouldReturnOkButNotCreateAccountsForDuplicates() {
        File mockBulkUploadFile = new File(mockFile);

        Response response = doPostMultipartForBulk(BULK_UPLOAD_URL, bearer, issuerId, mockBulkUploadFile);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.jsonPath().getList("CREATED_ACCOUNTS").isEmpty()).isTrue();
        assertThat(response.jsonPath().getList("ERRORED_ACCOUNTS").isEmpty()).isTrue();
    }
}
