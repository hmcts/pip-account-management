package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.storage.blob.models.BlobStorageException;
import com.microsoft.graph.models.MacOSOfficeSuiteApp;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.database.AzureBlobService;
import uk.gov.hmcts.reform.pip.account.management.database.MediaLegalApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeGenericLog;
import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class MediaLegalApplicationService {

    private final MediaLegalApplicationRepository mediaLegalApplicationRepository;

    private final AzureBlobService azureBlobService;

    LocalDateTime now = LocalDateTime.now();

    @Autowired
    public MediaLegalApplicationService(MediaLegalApplicationRepository mediaLegalApplicationRepository,
                                        AzureBlobService azureBlobService) {
        this.mediaLegalApplicationRepository = mediaLegalApplicationRepository;
        this.azureBlobService = azureBlobService;
    }

    /**
     * Get a list of all the applications.
     *
     * @return A list of all applications
     */
    public List<MediaAndLegalApplication> getApplications() {
        return mediaLegalApplicationRepository.findAll();
    }

    /**
     * Get a list of applications by the status.
     *
     * @param status The MediaLegalApplicationStatus enum to retrieve applications by
     * @return A list of all applications with the relevant status
     */
    public List<MediaAndLegalApplication> getApplicationsByStatus(MediaLegalApplicationStatus status) {
        return mediaLegalApplicationRepository.findByStatus(status);
    }

    /**
     * Get an application by the application id.
     *
     * @param id The id of the application
     * @return The application if it exists
     */
    public MediaAndLegalApplication getApplicationById(UUID id) {
        return mediaLegalApplicationRepository.findById(id).orElseThrow(() ->
            new NotFoundException(String.format("Application with id %s could not be found", id)));
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
     * @param file The file to upload to the blob store
     * @return The newly created application
     */
    public MediaAndLegalApplication createApplication(MediaAndLegalApplication application, MultipartFile file) {

        log.info(writeLog(application.getEmail(), UserActions.CREATE_MEDIA_APPLICATION, application.getEmail()));

        String imageId = azureBlobService.uploadFile(UUID.randomUUID().toString(), file);

        application.setFullName(StringUtils.trimAllWhitespace(application.getFullName()));
        application.setRequestDate(now);
        application.setStatusDate(now);
        application.setImage(imageId);
        application.setImageName(file.getOriginalFilename());

        return mediaLegalApplicationRepository.save(application);
    }

    /**
     * Update an application by fetching the application.
     *
     * @param id The id of the application to update
     * @param status The status to update the application with
     * @return The updated application
     */
    public MediaAndLegalApplication updateApplication(UUID id, MediaLegalApplicationStatus status) {
        MediaAndLegalApplication applicationToUpdate = mediaLegalApplicationRepository.findById(id).orElseThrow(() ->
            new NotFoundException(String.format("Application with id %s could not be found", id)));

        log.info(writeLog(UserActions.UPDATE_MEDIA_APPLICATION, applicationToUpdate.getEmail()));

        applicationToUpdate.setStatus(status);
        applicationToUpdate.setStatusDate(now);

        return mediaLegalApplicationRepository.save(applicationToUpdate);
    }

    /**
     * Delete an application by fetching the application entity to ensure the correct blob will be deleted.
     *
     * @param id The id of the application to delete
     */
    public void deleteApplication(UUID id) {
        MediaAndLegalApplication applicationToDelete = mediaLegalApplicationRepository.findById(id).orElseThrow(() ->
            new NotFoundException(String.format("Application with id %s could not be found", id)));

        log.info(writeLog(UserActions.DELETE_MEDIA_APPLICATION, applicationToDelete.getEmail()));

        azureBlobService.deleteBlob(applicationToDelete.getImage());
        mediaLegalApplicationRepository.delete(applicationToDelete);
    }
}
