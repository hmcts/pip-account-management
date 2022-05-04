package uk.gov.hmcts.reform.pip.account.management.database;

import java.io.IOException;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.FileException;

@Component
public class AzureBlobService {

    private final BlobContainerClient blobContainerClient;

    private static final String DELETE_MESSAGE = "Blob: %s successfully deleted.";

    @Autowired
    public AzureBlobService(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    /**
     * Uploads the file in the Azure blob service.
     *
     * @param payloadId The identifier of the payload
     * @param file  The file to upload
     * @return The URL where the file was uploaded.
     */
    public String uploadFile(String payloadId, MultipartFile file) {
        BlobClient blobClient = blobContainerClient.getBlobClient(payloadId);

        try {
            blobClient.upload(file.getInputStream(), file.getSize(), true);
        } catch (IOException e) {
            throw new FileException("Could not parse provided file, please check support file types and try again");
        }
        return blobContainerClient.getBlobContainerUrl() + "/" + payloadId;
    }

    /**
     * Gets the data held within a blob from the blob service.
     *
     * @param payloadId the identifier of the payload
     * @return the data contained within the blob in String format.
     */
    public String getBlobData(String payloadId) {
        BlobClient blobClient = blobContainerClient.getBlobClient(payloadId);
        return blobClient.downloadContent().toString();
    }

    public Resource getBlobFile(String payloadId) {
        BlobClient blobClient = blobContainerClient.getBlobClient(payloadId);
        byte[] data = blobClient.downloadContent().toBytes();
        return new ByteArrayResource(data);
    }

    public String deleteBlob(String payloadId) {
        BlobClient blobClient = blobContainerClient.getBlobClient(payloadId);
        blobClient.delete();
        return String.format(DELETE_MESSAGE, payloadId);
    }
}
