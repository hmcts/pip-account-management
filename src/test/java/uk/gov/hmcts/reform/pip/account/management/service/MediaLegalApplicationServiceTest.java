package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.AzureBlobService;
import uk.gov.hmcts.reform.pip.account.management.database.MediaLegalApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.helper.MediaLegalApplicationHelper;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;

import java.util.List;
import java.util.Optional;

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

    private final MediaLegalApplicationHelper helper = new MediaLegalApplicationHelper();
    private MediaAndLegalApplication mediaAndLegalApplicationExample = new MediaAndLegalApplication();
    private MediaAndLegalApplication mediaAndLegalApplicationExampleWithImageUrl = new MediaAndLegalApplication();
    private static final String FORMATTED_FULL_NAME = "TestUser";
    private static final MediaLegalApplicationStatus UPDATED_STATUS = MediaLegalApplicationStatus.APPROVED;
    private static final String BLOB_UUID = "uuidTest";


    @BeforeEach
    void setup() {
        mediaAndLegalApplicationExample = helper.createApplication(helper.STATUS);

        mediaAndLegalApplicationExampleWithImageUrl = mediaAndLegalApplicationExample;
        mediaAndLegalApplicationExampleWithImageUrl.setImage(BLOB_UUID);
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
        when(mediaLegalApplicationRepository.findByStatus(helper.STATUS))
            .thenReturn(List.of(mediaAndLegalApplicationExample));
        List<MediaAndLegalApplication> returnedApplications =
            mediaLegalApplicationService.getApplicationsByStatus(helper.STATUS);

        assertTrue(returnedApplications.contains(mediaAndLegalApplicationExample),
                   "Example application not contained in list");

        assertEquals(helper.STATUS, returnedApplications.get(0).getStatus(),
                     "Status filter was not applied");
    }

    @Test
    void testGetApplicationById() {
        when(mediaLegalApplicationRepository.findById(helper.TEST_ID))
            .thenReturn(Optional.ofNullable(mediaAndLegalApplicationExample));

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService.getApplicationById(helper.TEST_ID);

        assertEquals(mediaAndLegalApplicationExample, returnedApplication,
                     "Returned application does not match");
    }

    @Test
    void testCreateApplication() {
        when(azureBlobService.uploadFile(any(), eq(helper.FILE))).thenReturn(BLOB_UUID);

        when(mediaLegalApplicationRepository.save(mediaAndLegalApplicationExample))
            .thenReturn(mediaAndLegalApplicationExample);

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService
            .createApplication(mediaAndLegalApplicationExample, helper.FILE);

        assertEquals(BLOB_UUID, returnedApplication.getImage(), "Image uuid does not match");
        assertEquals(FORMATTED_FULL_NAME, returnedApplication.getFullName(),
                     "Full name has not been formatted");
        assertNotNull(returnedApplication.getRequestDate(), "Request date has not been set");
        assertNotNull(returnedApplication.getStatusDate(), "Status date has not been set");
    }

    @Test
    void testUpdateApplication() {
        when(mediaLegalApplicationRepository.getById(helper.TEST_ID)).thenReturn(mediaAndLegalApplicationExample);

        when(mediaLegalApplicationRepository.save(mediaAndLegalApplicationExample))
            .thenReturn(mediaAndLegalApplicationExample);

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService
            .updateApplication(helper.TEST_ID, MediaLegalApplicationStatus.APPROVED);

        assertEquals(UPDATED_STATUS, returnedApplication.getStatus(), "Application status was not updated");
    }

    @Test
    void testDeleteApplication() {

        when(mediaLegalApplicationRepository.getById(helper.TEST_ID))
            .thenReturn(mediaAndLegalApplicationExampleWithImageUrl);
        when(azureBlobService.deleteBlob(BLOB_UUID)).thenReturn("Blob successfully deleted");

        doNothing().when(mediaLegalApplicationRepository).delete(mediaAndLegalApplicationExampleWithImageUrl);

        mediaLegalApplicationService.deleteApplication(helper.TEST_ID);

        verify(mediaLegalApplicationRepository, times(1))
            .delete(mediaAndLegalApplicationExampleWithImageUrl);
    }
}
