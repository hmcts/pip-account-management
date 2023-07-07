package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.storage.blob.models.BlobStorageException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.database.AzureBlobService;
import uk.gov.hmcts.reform.pip.account.management.database.MediaApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus.APPROVED;
import static uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus.PENDING;
import static uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus.REJECTED;
import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Slf4j
@Service
public class MediaApplicationService {

    private final MediaApplicationRepository mediaApplicationRepository;

    private final AzureBlobService azureBlobService;
    private final PublicationService publicationService;

    private static final String APPLICATION_NOT_FOUND = "Application with id %s could not be found";

    @Autowired
    public MediaApplicationService(MediaApplicationRepository mediaApplicationRepository,
                                   AzureBlobService azureBlobService,
                                   PublicationService publicationService) {
        this.mediaApplicationRepository = mediaApplicationRepository;
        this.azureBlobService = azureBlobService;
        this.publicationService = publicationService;
    }

    /**
     * Get a list of all the applications.
     *
     * @return A list of all applications
     */
    public List<MediaApplication> getApplications() {
        return mediaApplicationRepository.findAll();
    }

    /**
     * Get a list of applications by the status.
     *
     * @param status The MediaApplicationStatus enum to retrieve applications by
     * @return A list of all applications with the relevant status
     */
    public List<MediaApplication> getApplicationsByStatus(MediaApplicationStatus status) {
        return mediaApplicationRepository.findByStatus(status);
    }

    /**
     * Get an application by the application id.
     *
     * @param id The id of the application
     * @return The application if it exists
     */
    public MediaApplication getApplicationById(UUID id) {
        return mediaApplicationRepository.findById(id).orElseThrow(() ->
                new NotFoundException(String.format(
                        APPLICATION_NOT_FOUND,
                        id
                )));
    }

    /**
     * Get an image from the blob store by the image id.
     *
     * @param imageId The id of the image to retrieve
     * @return The file if found
     */
    public Resource getImageById(String imageId) {
        try {
            return azureBlobService.getBlobFile(imageId);
        } catch (BlobStorageException e) {
            throw new NotFoundException(String.format("Image with id %s could not be found", imageId));
        }
    }

    /**
     * Create an application and store the image in the blob store, saving the blob url in the entity.
     *
     * @param application The application entity to save to the database
     * @param file        The file to upload to the blob store
     * @return The newly created application
     */
    public MediaApplication createApplication(MediaApplication application, MultipartFile file) {
        String imageId = azureBlobService.uploadFile(UUID.randomUUID().toString(), file);
        application.setRequestDate(LocalDateTime.now());
        application.setStatusDate(LocalDateTime.now());
        application.setImage(imageId);
        application.setImageName(file.getOriginalFilename());

        MediaApplication createdMediaApplication = mediaApplicationRepository.save(application);

        log.info(writeLog(createdMediaApplication.getId().toString(), UserActions.CREATE_MEDIA_APPLICATION,
                createdMediaApplication.getId().toString()
        ));

        return createdMediaApplication;
    }

    /**
     * Update an application by fetching the application.
     * If the status has been updated to Approved or Rejected then also delete the stored image.
     *
     * @param id     The id of the application to update
     * @param status The status to update the application with
     * @return The updated application
     */
    public MediaApplication updateApplication(UUID id, MediaApplicationStatus status) {
        MediaApplication applicationToUpdate = mediaApplicationRepository
                .findById(id).orElseThrow(() -> new NotFoundException(
                        String.format(
                                APPLICATION_NOT_FOUND,
                                id
                        )));

        log.info(writeLog(UserActions.UPDATE_MEDIA_APPLICATION, applicationToUpdate.getId().toString()));

        applicationToUpdate.setStatus(status);
        applicationToUpdate.setStatusDate(LocalDateTime.now());

        if (APPROVED.equals(status) || REJECTED.equals(status)) {
            azureBlobService.deleteBlob(applicationToUpdate.getImage());
        }

        return mediaApplicationRepository.save(applicationToUpdate);
    }

    public MediaApplication updateApplication(UUID id, MediaApplicationStatus status,
                                              Map<String, List<String>> reasons) {
        MediaApplication applicationToUpdate = mediaApplicationRepository
                .findById(id).orElseThrow(() -> new NotFoundException(
                        String.format(
                                APPLICATION_NOT_FOUND,
                                id
                        )));
        if (REJECTED.equals(status)) {
            sendMediaApplicationRejectionEmail(id, reasons);
        }

        log.info(writeLog(UserActions.UPDATE_MEDIA_APPLICATION, applicationToUpdate.getId().toString()));

        applicationToUpdate.setStatus(status);
        applicationToUpdate.setStatusDate(LocalDateTime.now());

        if (APPROVED.equals(status) || REJECTED.equals(status)) {
            azureBlobService.deleteBlob(applicationToUpdate.getImage());
        }

        return mediaApplicationRepository.save(applicationToUpdate);
    }

    /**
     * Delete an application by fetching the application entity to ensure the correct blob will be deleted.
     *
     * @param id The id of the application to delete
     */
    public void deleteApplication(UUID id) {
        MediaApplication applicationToDelete = mediaApplicationRepository
                .findById(id).orElseThrow(() -> new NotFoundException(String.format(APPLICATION_NOT_FOUND, id)));
        log.info(writeLog(UserActions.DELETE_MEDIA_APPLICATION, applicationToDelete.getId().toString()));

        azureBlobService.deleteBlob(applicationToDelete.getImage());
        mediaApplicationRepository.delete(applicationToDelete);
    }

    public String deleteAllApplicationsWithEmailPrefix(String prefix) {
        List<MediaApplication> applicationsToDelete = mediaApplicationRepository
            .findAllByEmailStartingWithIgnoreCase(prefix);

        if (!applicationsToDelete.isEmpty()) {
            applicationsToDelete.forEach(a -> {
                if (PENDING.equals(a.getStatus())) {
                    azureBlobService.deleteBlob(a.getImage());
                }
            });

            List<UUID> applicationIds = applicationsToDelete.stream()
                .map(MediaApplication::getId)
                .toList();
            mediaApplicationRepository.deleteByIdIn(applicationIds);
        }
        return String.format("%s media application(s) deleted with email starting with %s",
                             applicationsToDelete.size(), prefix);
    }

    /**
     * Send rejection email for a given applicant.
     */
    private void sendMediaApplicationRejectionEmail(UUID id, Map<String, List<String>> rejectionReasons) {
        MediaApplication mediaApplication = this.getApplicationById(id);
        publicationService.sendMediaAccountRejectionEmail(mediaApplication, rejectionReasons);
    }

    /**
     * Collate media applications and send them for reporting.
     */
    public void processApplicationsForReporting() {
        List<MediaApplication> mediaApplications = getApplications();
        if (!mediaApplications.isEmpty()) {
            publicationService.sendMediaApplicationReportingEmail(mediaApplications);
            processApplicationsForDeleting(mediaApplications);
        }
    }

    /**
     * Delete media applications that have APPROVED or REJECTED status.
     */
    private void processApplicationsForDeleting(List<MediaApplication> mediaApplications) {
        mediaApplicationRepository.deleteAllInBatch(
                mediaApplications.stream().filter(app -> app.getStatus().equals(APPROVED)
                                || app.getStatus().equals(REJECTED))
                        .toList());

        log.info(writeLog("Approved and Rejected media applications deleted"));
    }
}
