package uk.gov.hmcts.reform.pip.account.management.database;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.FileException;

import java.io.IOException;

/**
 * Class with handles the interaction with the Azure Blob Service.
 */
@Component
public class AzureBlobService {

    private final BlobContainerClient blobContainerClient;

    private static final String DELETE_MESSAGE = "Blob: %s successfully deleted.";

    @Autowired
    public AzureBlobService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    /**
     * Uploads the image in the Azure blob service.
     *
     * @param imageId The identifier of the image
     * @param file  The file to upload
     * @return The id linked to the uploaded image
     */
    public String uploadFile(String imageId, MultipartFile file) {
        BlobClient blobClient = blobContainerClient.getBlobClient(imageId);

        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);
        } catch (IOException e) {
            throw new FileException("Could not parse provided file, please check support file types and try again");
        }
        return imageId;
    }

    /**
     * Get the file from the blobstore by the imageId.
     *
     * @param imageId The id of the file to retrieve
     * @return The file from the blob store
     */
    public Resource getBlobFile(String imageId) {
        BlobClient blobClient = blobContainerClient.getBlobClient(imageId);
        byte[] data = blobClient.downloadContent().toBytes();
        return new ByteArrayResource(data);
    }

    /**
     * Delete a blob from the blob store by the imageId.
     *
     * @param imageId The id of the blob to delete
     * @return A confirmation message of the blob deletion
     */
    public String deleteBlob(String imageId) {
        BlobClient blobClient = blobContainerClient.getBlobClient(imageId);
        blobClient.delete();
        return String.format(DELETE_MESSAGE, imageId);
    }
}
