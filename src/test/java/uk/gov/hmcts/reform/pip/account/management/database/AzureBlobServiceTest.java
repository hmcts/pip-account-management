package uk.gov.hmcts.reform.pip.account.management.database;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureBlobServiceTest {
    private static final MultipartFile FILE = new MockMultipartFile("test", (byte[]) null);
    private static final String BLOB_NAME = UUID.randomUUID().toString();

    private static final String MESSAGES_MATCH = "Messages should match";

    @Mock
    BlobContainerClient blobContainerClient;

    @Mock
    BlobClient blobClient;

    @InjectMocks
    AzureBlobService azureBlobService;

    @BeforeEach
    void setup() {
        when(blobContainerClient.getBlobClient(BLOB_NAME)).thenReturn(blobClient);
    }

    @Test
    void testCreationOfNewBlobViaFile() {
        String blobId = azureBlobService.uploadFile(BLOB_NAME, FILE);

        assertEquals(BLOB_NAME, blobId, "Image id does not"
            + "contain the correct value");
    }

    @Test
    void testGetBlobFile() {
        BinaryData binaryData = BinaryData.fromString("TestString");
        when(blobClient.downloadContent()).thenReturn(binaryData);
        Resource blobFile = azureBlobService.getBlobFile(BLOB_NAME);
        byte[] data = binaryData.toBytes();
        assertEquals(blobFile, new ByteArrayResource(data), "Wrong data returned.");
    }

    @Test
    void testDeleteBlob() {
        assertEquals(String.format("Blob: %s successfully deleted.", BLOB_NAME),
                     azureBlobService.deleteBlob(BLOB_NAME),
                     MESSAGES_MATCH);
    }
}
