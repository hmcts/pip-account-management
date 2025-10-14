package uk.gov.hmcts.reform.pip.account.management;

import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class BulkAccountTest extends AccountHelperBase {
    private String mockFile;
    private Map<String, String> issuerId;
    private String systemAdminId;

    @BeforeAll
    void startUp() throws IOException {
        PiUser systemAdminUser = createSystemAdminAccount();
        systemAdminId = systemAdminUser.getUserId();

        issuerId = Map.of(REQUESTER_ID_HEADER, systemAdminId);

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
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, systemAdminId);
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, headers);
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
