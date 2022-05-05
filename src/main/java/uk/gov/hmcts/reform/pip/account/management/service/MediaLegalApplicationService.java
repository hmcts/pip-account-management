package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.database.AzureBlobService;
import uk.gov.hmcts.reform.pip.account.management.database.MediaLegalApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
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
        return mediaLegalApplicationRepository.findByStatus(status.toString());
    }

    public MediaAndLegalApplication getApplicationById(UUID id) {
        return mediaLegalApplicationRepository.getById(id);
    }

    /**
     * Create an application and store the image in the blob store, saving the blob url in the entity.
     *
     * @param application The application entity to save to the database
     * @param file The file to upload to the blob store
     * @return The newly created application
     */
    public MediaAndLegalApplication createApplication(MediaAndLegalApplication application, MultipartFile file) {
        String blobUrl = azureBlobService.uploadFile(UUID.randomUUID().toString(), file);

        application.setFullName(StringUtils.trimAllWhitespace(application.getFullName()));
        application.setRequestDate(now);
        application.setStatusDate(now);
        application.setImage(blobUrl);

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
        MediaAndLegalApplication applicationToUpdate = mediaLegalApplicationRepository.getById(id);
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
        MediaAndLegalApplication fetchedApplication = mediaLegalApplicationRepository.getById(id);
        azureBlobService.deleteBlob(getUuidFromUrl(fetchedApplication.getImage()));
        mediaLegalApplicationRepository.delete(fetchedApplication);
    }

    /**
     * Pass in the stored url and get the UUID from it.
     *
     * @param payloadUrl The url to get the UUID from
     * @return the UUID of the blob
     */
    private String getUuidFromUrl(String payloadUrl) {
        return payloadUrl.substring(payloadUrl.lastIndexOf('/') + 1);
    }
}
