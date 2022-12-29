package uk.gov.hmcts.reform.pip.account.management.config;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

/**
 * Mock class for testing to mock out external calls to Azure.
 */
@Configuration
@Profile("test")
public class AzureBlobConfigurationTest {

    @Mock
    BlobClient blobClientMock;

    @Mock
    BlobContainerClient blobContainerClientMock;

    public AzureBlobConfigurationTest() {
        MockitoAnnotations.openMocks(this);
    }

    @Bean
    public BlobContainerClient blobContainerClient() {
        return blobContainerClientMock;
    }

    @Bean
    public BlobClient blobClient() {
        return blobClientMock;
    }

    @Mock
    AzureBlobConfigurationProperties azureBlobConfigurationProperties;

    @InjectMocks
    AzureBlobConfiguration azureBlobConfiguration;

    @Test
    void testBlobContainerClientBuilder() {
        when(azureBlobConfigurationProperties.getConnectionString()).thenReturn(
            "DefaultEndpointsProtocol=https;AccountName=test;AccountKey=test;EndpointSuffix=core.windows.net");
        when(azureBlobConfigurationProperties.getContainerName()).thenReturn("12345");

        BlobContainerClient blobContainerClient =
            azureBlobConfiguration.blobContainerClient(azureBlobConfigurationProperties);

        assertNotNull(blobContainerClient, "Blob container created successfully");
    }
}

