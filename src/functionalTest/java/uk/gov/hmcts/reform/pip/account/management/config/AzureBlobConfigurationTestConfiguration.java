package uk.gov.hmcts.reform.pip.account.management.config;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * These are required in the functional test folder to allow the spring context to load, which is
 * needed to get access to some of the autowired properties.
 * Note - The test itself does not use this mock, and is calling a real implementation.
 */
@Configuration
@Profile("functional")
public class AzureBlobConfigurationTestConfiguration {

    @Mock
    BlobClient blobClientMock;

    @Mock
    BlobContainerClient blobContainerClientMock;

    public AzureBlobConfigurationTestConfiguration() {
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
}

