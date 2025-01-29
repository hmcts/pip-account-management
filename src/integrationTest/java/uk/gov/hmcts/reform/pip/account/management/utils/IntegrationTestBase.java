package uk.gov.hmcts.reform.pip.account.management.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.hmcts.reform.pip.account.management.service.DataManagementService;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class IntegrationTestBase {
    private static final String BLOB_IMAGE_URL = "https://localhost";

    @MockitoBean
    protected PublicationService publicationService;

    @MockitoBean
    protected DataManagementService dataManagementService;

    @MockitoBean
    protected BlobContainerClient blobContainerClient;

    @MockitoBean
    protected BlobClient blobClient;

    @BeforeEach
    void setupBlobClient() {
        when(blobContainerClient.getBlobClient(any())).thenReturn(blobClient);
        when(blobContainerClient.getBlobContainerUrl()).thenReturn(BLOB_IMAGE_URL);
    }
}
