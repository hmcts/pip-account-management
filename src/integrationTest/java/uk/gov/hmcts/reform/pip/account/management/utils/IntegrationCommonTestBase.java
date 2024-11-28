package uk.gov.hmcts.reform.pip.account.management.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class IntegrationCommonTestBase {
    private static final String BLOB_IMAGE_URL = "https://localhost";

    @MockBean
    protected BlobContainerClient blobContainerClient;

    @MockBean
    protected BlobClient blobClient;

    @BeforeEach
    void setupBlobClient() {
        when(blobContainerClient.getBlobClient(any())).thenReturn(blobClient);
        when(blobContainerClient.getBlobContainerUrl()).thenReturn(BLOB_IMAGE_URL);
    }
}
