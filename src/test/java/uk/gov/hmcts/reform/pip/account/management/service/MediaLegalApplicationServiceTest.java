package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.storage.blob.models.BlobStorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.reform.pip.account.management.database.AzureBlobService;
import uk.gov.hmcts.reform.pip.account.management.database.MediaLegalApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaLegalApplicationHelper.FILE;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaLegalApplicationHelper.STATUS;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaLegalApplicationHelper.TEST_ID;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaLegalApplicationHelper.createApplication;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class MediaLegalApplicationServiceTest {

    @Mock
    private MediaLegalApplicationRepository mediaLegalApplicationRepository;

    @Mock
    private AzureBlobService azureBlobService;

    @InjectMocks
    private MediaLegalApplicationService mediaLegalApplicationService;

    private MediaAndLegalApplication mediaAndLegalApplicationExample = new MediaAndLegalApplication();
    private MediaAndLegalApplication mediaAndLegalApplicationExampleWithImageUrl = new MediaAndLegalApplication();
    private static final String FORMATTED_FULL_NAME = "TestUser";
    private static final MediaLegalApplicationStatus UPDATED_STATUS = MediaLegalApplicationStatus.APPROVED;
    private static final String BLOB_UUID = "uuidTest";
    private static final String IMAGE_NAME = "test-image.png";
    private static final String NOT_FOUND_MESSAGE = "Not found exception does not contain expected ID";
    private static final String NOT_FOUND_EXCEPTION_THROWN_MESSAGE = "Expected NotFoundException to be thrown";


    @BeforeEach
    void setup() {
        mediaAndLegalApplicationExample = createApplication(STATUS);

        mediaAndLegalApplicationExampleWithImageUrl = mediaAndLegalApplicationExample;
        mediaAndLegalApplicationExampleWithImageUrl.setImage(BLOB_UUID);
        mediaAndLegalApplicationExampleWithImageUrl.setImageName(IMAGE_NAME);
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
        when(mediaLegalApplicationRepository.findByStatus(STATUS))
            .thenReturn(List.of(mediaAndLegalApplicationExample));
        List<MediaAndLegalApplication> returnedApplications =
            mediaLegalApplicationService.getApplicationsByStatus(STATUS);

        assertTrue(returnedApplications.contains(mediaAndLegalApplicationExample),
                   "Example application not contained in list");

        assertEquals(STATUS, returnedApplications.get(0).getStatus(),
                     "Status filter was not applied");
    }

    @Test
    void testGetApplicationById() {
        when(mediaLegalApplicationRepository.findById(TEST_ID))
            .thenReturn(Optional.ofNullable(mediaAndLegalApplicationExample));

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService.getApplicationById(TEST_ID);

        assertEquals(mediaAndLegalApplicationExample, returnedApplication,
                     "Returned application does not match");
    }

    @Test
    void testGetApplicationByIdNotFound() {
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaLegalApplicationService.getApplicationById(TEST_ID), NOT_FOUND_EXCEPTION_THROWN_MESSAGE
        );

        assertTrue(notFoundException.getMessage().contains(String.valueOf(TEST_ID)), NOT_FOUND_MESSAGE);
    }

    @Test
    void testGetImageById() {
        when(azureBlobService.getBlobFile(BLOB_UUID)).thenReturn(FILE.getResource());

        Resource returnedResource = mediaLegalApplicationService.getImageById(BLOB_UUID);

        assertNotNull(returnedResource, "Returned resource should not be null");
    }

    @Test
    void testGetImageByIdNotFound() {
        when(azureBlobService.getBlobFile(BLOB_UUID)).thenThrow(BlobStorageException.class);

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaLegalApplicationService.getImageById(BLOB_UUID), NOT_FOUND_EXCEPTION_THROWN_MESSAGE
        );

        assertTrue(notFoundException.getMessage().contains(BLOB_UUID), NOT_FOUND_MESSAGE);
    }

    @Test
    void testCreateApplication() {
        when(azureBlobService.uploadFile(any(), eq(FILE))).thenReturn(BLOB_UUID);

        when(mediaLegalApplicationRepository.save(mediaAndLegalApplicationExample))
            .thenReturn(mediaAndLegalApplicationExample);

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService
            .createApplication(mediaAndLegalApplicationExample, FILE);

        assertEquals(BLOB_UUID, returnedApplication.getImage(), "Image uuid does not match");
        assertEquals(FORMATTED_FULL_NAME, returnedApplication.getFullName(),
                     "Full name has not been formatted");
        assertNotNull(returnedApplication.getRequestDate(), "Request date has not been set");
        assertNotNull(returnedApplication.getStatusDate(), "Status date has not been set");
    }

    @Test
    void testUpdateApplication() {
        when(mediaLegalApplicationRepository.findById(TEST_ID)).thenReturn(Optional.ofNullable(
            mediaAndLegalApplicationExample));

        when(mediaLegalApplicationRepository.save(mediaAndLegalApplicationExample))
            .thenReturn(mediaAndLegalApplicationExample);

        MediaAndLegalApplication returnedApplication = mediaLegalApplicationService
            .updateApplication(TEST_ID, MediaLegalApplicationStatus.APPROVED);

        assertEquals(UPDATED_STATUS, returnedApplication.getStatus(), "Application status was not updated");
    }

    @Test
    void testUpdateApplicationNotFound() {
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaLegalApplicationService.updateApplication(TEST_ID, STATUS), NOT_FOUND_EXCEPTION_THROWN_MESSAGE);

        assertTrue(notFoundException.getMessage().contains(String.valueOf(TEST_ID)), NOT_FOUND_MESSAGE);
    }

    @Test
    void testDeleteApplication() {

        when(mediaLegalApplicationRepository.findById(TEST_ID))
            .thenReturn(Optional.ofNullable(mediaAndLegalApplicationExampleWithImageUrl));
        when(azureBlobService.deleteBlob(BLOB_UUID)).thenReturn("Blob successfully deleted");

        doNothing().when(mediaLegalApplicationRepository).delete(mediaAndLegalApplicationExampleWithImageUrl);

        mediaLegalApplicationService.deleteApplication(TEST_ID);

        verify(mediaLegalApplicationRepository, times(1))
            .delete(mediaAndLegalApplicationExampleWithImageUrl);
    }

    @Test
    void testDeleteApplicationNotFound() {
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaLegalApplicationService.deleteApplication(TEST_ID), NOT_FOUND_EXCEPTION_THROWN_MESSAGE);

        assertTrue(notFoundException.getMessage().contains(String.valueOf(TEST_ID)), NOT_FOUND_MESSAGE);
    }
}
