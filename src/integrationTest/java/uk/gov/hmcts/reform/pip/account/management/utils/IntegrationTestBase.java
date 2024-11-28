package uk.gov.hmcts.reform.pip.account.management.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.account.management.service.SubscriptionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class IntegrationTestBase {
    private static final String BLOB_IMAGE_URL = "https://localhost";

    @MockBean
    protected SubscriptionService subscriptionService;

    @MockBean
    protected PublicationService publicationService;

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
