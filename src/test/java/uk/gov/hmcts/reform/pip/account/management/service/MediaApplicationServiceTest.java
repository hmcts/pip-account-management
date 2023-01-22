package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.storage.blob.models.BlobStorageException;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.reform.pip.account.management.database.AzureBlobService;
import uk.gov.hmcts.reform.pip.account.management.database.MediaApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.FILE;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.STATUS;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.TEST_ID;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.createApplication;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class MediaApplicationServiceTest {

    @Mock
    private MediaApplicationRepository mediaApplicationRepository;

    @Mock
    private AzureBlobService azureBlobService;

    @Mock
    private PublicationService publicationService;

    @InjectMocks
    private MediaApplicationService mediaApplicationService;

    LogCaptor logCaptor = LogCaptor.forClass(MediaApplicationService.class);

    private MediaApplication mediaApplicationExample = new MediaApplication();
    private MediaApplication mediaApplicationExampleWithImageUrl = new MediaApplication();
    private static final String FORMATTED_FULL_NAME = "Test User";
    private static final MediaApplicationStatus UPDATED_STATUS = MediaApplicationStatus.APPROVED;
    private static final String BLOB_UUID = "uuidTest";
    private static final String IMAGE_NAME = "test-image.png";
    private static final String NOT_FOUND_MESSAGE = "Not found exception does not contain expected ID";
    private static final String NOT_FOUND_EXCEPTION_THROWN_MESSAGE = "Expected NotFoundException to be thrown";
    private static final String EMAIL_SENT_MESSAGE = "Email sent";


    @BeforeEach
    void setup() {
        mediaApplicationExample = createApplication(STATUS);

        mediaApplicationExampleWithImageUrl = mediaApplicationExample;
        mediaApplicationExampleWithImageUrl.setImage(BLOB_UUID);
        mediaApplicationExampleWithImageUrl.setImageName(IMAGE_NAME);
    }

    @Test
    void testGetApplications() {
        when(mediaApplicationRepository.findAll()).thenReturn(List.of(mediaApplicationExample));
        List<MediaApplication> returnedApplications = mediaApplicationService.getApplications();

        assertTrue(returnedApplications.contains(mediaApplicationExample),
                   "Example application not contained in list");
    }

    @Test
    void testGetApplicationsByStatus() {
        when(mediaApplicationRepository.findByStatus(STATUS))
            .thenReturn(List.of(mediaApplicationExample));
        List<MediaApplication> returnedApplications =
            mediaApplicationService.getApplicationsByStatus(STATUS);

        assertTrue(returnedApplications.contains(mediaApplicationExample),
                   "Example application not contained in list");

        assertEquals(STATUS, returnedApplications.get(0).getStatus(),
                     "Status filter was not applied");
    }

    @Test
    void testGetApplicationById() {
        when(mediaApplicationRepository.findById(TEST_ID))
            .thenReturn(Optional.ofNullable(mediaApplicationExample));

        MediaApplication returnedApplication = mediaApplicationService.getApplicationById(TEST_ID);

        assertEquals(mediaApplicationExample, returnedApplication,
                     "Returned application does not match");
    }

    @Test
    void testGetApplicationByIdNotFound() {
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaApplicationService.getApplicationById(TEST_ID), NOT_FOUND_EXCEPTION_THROWN_MESSAGE
        );

        assertTrue(notFoundException.getMessage().contains(String.valueOf(TEST_ID)), NOT_FOUND_MESSAGE);
    }

    @Test
    void testGetImageById() {
        when(azureBlobService.getBlobFile(BLOB_UUID)).thenReturn(FILE.getResource());

        Resource returnedResource = mediaApplicationService.getImageById(BLOB_UUID);

        assertNotNull(returnedResource, "Returned resource should not be null");
    }

    @Test
    void testGetImageByIdNotFound() {
        when(azureBlobService.getBlobFile(BLOB_UUID)).thenThrow(BlobStorageException.class);

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaApplicationService.getImageById(BLOB_UUID), NOT_FOUND_EXCEPTION_THROWN_MESSAGE
        );

        assertTrue(notFoundException.getMessage().contains(BLOB_UUID), NOT_FOUND_MESSAGE);
    }

    @Test
    void testCreateApplication() {
        when(azureBlobService.uploadFile(any(), eq(FILE))).thenReturn(BLOB_UUID);

        when(mediaApplicationRepository.save(mediaApplicationExample))
            .thenReturn(mediaApplicationExample);

        MediaApplication returnedApplication = mediaApplicationService
            .createApplication(mediaApplicationExample, FILE);

        assertEquals(BLOB_UUID, returnedApplication.getImage(), "Image uuid does not match");
        assertEquals(FORMATTED_FULL_NAME, returnedApplication.getFullName(),
                     "Full name does not match");
        assertNotNull(returnedApplication.getRequestDate(), "Request date has not been set");
        assertNotNull(returnedApplication.getStatusDate(), "Status date has not been set");
    }

    @Test
    void testUpdateApplicationApproved() {
        when(mediaApplicationRepository.findById(TEST_ID)).thenReturn(Optional.ofNullable(
            mediaApplicationExample));

        when(mediaApplicationRepository.save(mediaApplicationExample))
            .thenReturn(mediaApplicationExample);

        MediaApplication returnedApplication = mediaApplicationService
            .updateApplication(TEST_ID, MediaApplicationStatus.APPROVED);

        assertEquals(UPDATED_STATUS, returnedApplication.getStatus(), "Application status was not updated");

        verify(azureBlobService, times(1)).deleteBlob(BLOB_UUID);
    }

    @Test
    void testUpdateApplicationRejected() {
        when(mediaApplicationRepository.findById(TEST_ID)).thenReturn(Optional.ofNullable(
            mediaApplicationExample));

        when(mediaApplicationRepository.save(mediaApplicationExample))
            .thenReturn(mediaApplicationExample);

        MediaApplication returnedApplication = mediaApplicationService
            .updateApplication(TEST_ID, MediaApplicationStatus.REJECTED);

        assertEquals(MediaApplicationStatus.REJECTED, returnedApplication.getStatus(),
                     "Application status was not updated");

        verify(azureBlobService, times(1)).deleteBlob(BLOB_UUID);
    }

    @Test
    void testUpdateApplicationPending() {
        when(mediaApplicationRepository.findById(TEST_ID)).thenReturn(Optional.ofNullable(
            mediaApplicationExample));

        when(mediaApplicationRepository.save(mediaApplicationExample))
            .thenReturn(mediaApplicationExample);

        MediaApplication returnedApplication = mediaApplicationService
            .updateApplication(TEST_ID, MediaApplicationStatus.PENDING);

        assertEquals(MediaApplicationStatus.PENDING, returnedApplication.getStatus(),
                     "Application status was not updated");

        verify(azureBlobService, times(0)).deleteBlob(BLOB_UUID);
    }

    @Test
    void testUpdateApplicationNotFound() {
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaApplicationService.updateApplication(TEST_ID, STATUS), NOT_FOUND_EXCEPTION_THROWN_MESSAGE);

        assertTrue(notFoundException.getMessage().contains(String.valueOf(TEST_ID)), NOT_FOUND_MESSAGE);
    }

    @Test
    void testDeleteApplication() {

        when(mediaApplicationRepository.findById(TEST_ID))
            .thenReturn(Optional.ofNullable(mediaApplicationExampleWithImageUrl));
        when(azureBlobService.deleteBlob(BLOB_UUID)).thenReturn("Blob successfully deleted");

        doNothing().when(mediaApplicationRepository).delete(mediaApplicationExampleWithImageUrl);

        mediaApplicationService.deleteApplication(TEST_ID);

        verify(mediaApplicationRepository, times(1))
            .delete(mediaApplicationExampleWithImageUrl);
    }

    @Test
    void testDeleteApplicationNotFound() {
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            mediaApplicationService.deleteApplication(TEST_ID), NOT_FOUND_EXCEPTION_THROWN_MESSAGE);

        assertTrue(notFoundException.getMessage().contains(String.valueOf(TEST_ID)), NOT_FOUND_MESSAGE);
    }

    @Test
    void testProcessApplicationForReporting() {
        when(mediaApplicationRepository.findAll()).thenReturn(List.of(mediaApplicationExample));
        when(publicationService.sendMediaApplicationReportingEmail(List.of(mediaApplicationExample)))
            .thenReturn(EMAIL_SENT_MESSAGE);

        mediaApplicationService.processApplicationsForReporting();

        assertEquals(EMAIL_SENT_MESSAGE, logCaptor.getInfoLogs().get(0),
                   "Publication service response logs not being captured.");

        assertEquals("Approved and Rejected media applications deleted", logCaptor.getInfoLogs().get(1),
                   "Application deletion logs not being captured");

        verify(publicationService).sendMediaApplicationReportingEmail(List.of(mediaApplicationExample));
        verify(mediaApplicationRepository).deleteAllInBatch(Collections.emptyList());
    }

    @Test
    void testProcessApplicationForReportingWithProcessedApplications() {
        List<MediaApplication> applications = List.of(createApplication(MediaApplicationStatus.APPROVED),
                                                      createApplication(MediaApplicationStatus.REJECTED));
        when(mediaApplicationRepository.findAll()).thenReturn(applications);
        when(publicationService.sendMediaApplicationReportingEmail(applications))
            .thenReturn(EMAIL_SENT_MESSAGE);

        mediaApplicationService.processApplicationsForReporting();

        assertEquals(EMAIL_SENT_MESSAGE, logCaptor.getInfoLogs().get(0),
                     "Publication service response logs not being captured.");

        assertEquals("Approved and Rejected media applications deleted", logCaptor.getInfoLogs().get(1),
                     "Application deletion logs not being captured");

        verify(publicationService).sendMediaApplicationReportingEmail(applications);
        verify(mediaApplicationRepository).deleteAllInBatch(applications);
    }

    @Test
    void testProcessApplicationForReportingWithNoApplication() {
        when(mediaApplicationRepository.findAll()).thenReturn(Collections.emptyList());

        mediaApplicationService.processApplicationsForReporting();
        verifyNoInteractions(publicationService);
        verify(mediaApplicationRepository, never()).deleteAllInBatch(any());
    }
}
