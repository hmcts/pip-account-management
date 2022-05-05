package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.database.AzureBlobService;
import uk.gov.hmcts.reform.pip.account.management.database.MediaLegalApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaLegalApplicationServiceTest {

    @Mock
    private MediaLegalApplicationRepository mediaLegalApplicationRepository;

    @Mock
    private AzureBlobService azureBlobService;

    @InjectMocks
    private MediaLegalApplicationService mediaLegalApplicationService;

    private final MediaAndLegalApplication mediaAndLegalApplicationExample = new MediaAndLegalApplication();
    private final MediaAndLegalApplication mediaAndLegalApplicationExampleWithImageUrl = new MediaAndLegalApplication();
    private static final MultipartFile FILE = new MockMultipartFile("test", (byte[]) null);
    private static final UUID TEST_ID = UUID.randomUUID();
    private static final String FULL_NAME = "Test user";
    private static final String FORMATTED_FULL_NAME = "Testuser";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final MediaLegalApplicationStatus STATUS = MediaLegalApplicationStatus.PENDING;
    private static final MediaLegalApplicationStatus UPDATED_STATUS = MediaLegalApplicationStatus.APPROVED;
    private static final String IMAGE_URL = "https://testPayload/uuidTest";
    private static final String BLOB_UUID = "uuidTest";


    @BeforeEach
    void setup() {
        mediaAndLegalApplicationExample.setId(TEST_ID);
        mediaAndLegalApplicationExample.setFullName(FULL_NAME);
        mediaAndLegalApplicationExample.setEmail(EMAIL);
        mediaAndLegalApplicationExample.setEmployer(EMPLOYER);
        mediaAndLegalApplicationExample.setStatus(STATUS);

        mediaAndLegalApplicationExampleWithImageUrl.setId(TEST_ID);
        mediaAndLegalApplicationExampleWithImageUrl.setFullName(FULL_NAME);
        mediaAndLegalApplicationExampleWithImageUrl.setEmail(EMAIL);
        mediaAndLegalApplicationExampleWithImageUrl.setEmployer(EMPLOYER);
        mediaAndLegalApplicationExampleWithImageUrl.setStatus(STATUS);
        mediaAndLegalApplicationExampleWithImageUrl.setImage(IMAGE_URL);
    }

    @Test
    void testGetApplications() {
        when(mediaLegalApplicationRepository.findAll()).thenReturn(List.of(mediaAndLegalApplicationExample));
        List<MediaAndLegalApplication> returnedApplications = mediaLegalApplicationService.getApplications();

        assertTrue(returnedApplications.contains(mediaAndLegalApplicationExample),
                   "Example application not contained in list");
    }

    @Test
    void testGetApplicationsByStatus() {
        when(mediaLegalApplicationRepository.findByStatus(STATUS.toString()))
            .thenReturn(List.of(mediaAndLegalApplicationExample));
        List<MediaAndLegalApplication> returnedApplications =
            mediaLegalApplicationService.getApplicationsByStatus(STATUS);

        assertTrue(returnedApplications.contains(mediaAndLegalApplicationExample),
                   "Example application not contained in list");

        assertEquals(STATUS, returnedApplications.get(0).getStatus(),
                     "Status filter was not applied");
    }

    @Test
    void testCreateApplication() {
        when(azureBlobService.uploadFile(any(), eq(FILE))).thenReturn(IMAGE_URL);

        when(mediaLegalApplicationRepository.save(mediaAndLegalApplicationExample))
            .thenReturn(mediaAndLegalApplicationExample);

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService
            .createApplication(mediaAndLegalApplicationExample, FILE);

        assertEquals(IMAGE_URL, returnedApplication.getImage(), "Image url does not match");
        assertEquals(FORMATTED_FULL_NAME, returnedApplication.getFullName(),
                     "Full name has not been formatted");
        assertNotNull(returnedApplication.getRequestDate(), "Request date has not been set");
        assertNotNull(returnedApplication.getStatusDate(), "Status date has not been set");
    }

    @Test
    void testUpdateApplication() {
        when(mediaLegalApplicationRepository.getById(TEST_ID)).thenReturn(mediaAndLegalApplicationExample);

        when(mediaLegalApplicationRepository.save(mediaAndLegalApplicationExample))
            .thenReturn(mediaAndLegalApplicationExample);

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService
            .updateApplication(TEST_ID, MediaLegalApplicationStatus.APPROVED);

        assertEquals(UPDATED_STATUS, returnedApplication.getStatus(), "Application status was not updated");
    }

    @Test
    void testDeleteApplication() {

        when(mediaLegalApplicationRepository.getById(TEST_ID)).thenReturn(mediaAndLegalApplicationExampleWithImageUrl);
        when(azureBlobService.deleteBlob(BLOB_UUID)).thenReturn("Blob successfully deleted");

        doNothing().when(mediaLegalApplicationRepository).delete(mediaAndLegalApplicationExampleWithImageUrl);

        mediaLegalApplicationService.deleteApplication(TEST_ID);

        verify(mediaLegalApplicationRepository, times(1))
            .delete(mediaAndLegalApplicationExampleWithImageUrl);
    }
}
