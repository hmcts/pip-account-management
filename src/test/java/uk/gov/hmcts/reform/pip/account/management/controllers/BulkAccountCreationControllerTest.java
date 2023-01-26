package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.reform.pip.account.management.service.BulkAccountCreationService;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkAccountCreationControllerTest {
    private static final String TEST_EMAIL = "test@user.com";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    private BulkAccountCreationService bulkAccountCreationService;

    @InjectMocks
    private BulkAccountCreationController bulkAccountCreationController;

    @Test
    void testCreateMediaAccountsBulkReturnsOk() throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("csv/valid.csv")) {
            MultipartFile multipartFile = new MockMultipartFile("file",
                                                                "TestFileName", "text/plain",
                                                                IOUtils.toByteArray(is));
            when(bulkAccountCreationService.uploadMediaFromCsv(multipartFile, TEST_EMAIL))
                .thenReturn(new ConcurrentHashMap<>());

            assertEquals(
                HttpStatus.OK,
                bulkAccountCreationController.createMediaAccountsBulk(TEST_EMAIL, multipartFile).getStatusCode(),
                STATUS_CODE_MATCH);
        }
    }

    @Test
    void testCreateMediaAccountsBulkReturnsMap() throws IOException {
        try (InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("csv/valid.csv")) {
            MultipartFile multipartFile = new MockMultipartFile("file",
                                                                "TestFileName", "text/plain",
                                                                IOUtils.toByteArray(is));
            when(bulkAccountCreationService.uploadMediaFromCsv(multipartFile, TEST_EMAIL))
                .thenReturn(new ConcurrentHashMap<>());

            assertEquals(new ConcurrentHashMap<>(),
                         bulkAccountCreationController.createMediaAccountsBulk(TEST_EMAIL, multipartFile).getBody(),
                         "Maps should match");
        }
    }
}
