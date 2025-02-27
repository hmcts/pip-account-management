package uk.gov.hmcts.reform.pip.account.management.config;

import com.azure.storage.blob.BlobContainerClient;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.when;

/**
 * Mock class for testing to mock out external calls to Azure.
 */
@ExtendWith(MockitoExtension.class)
class AzureBlobConfigurationTest {
    private static final String[] BLOB_STORAGE_DEV_PROFILE = {"blobStorageDev"};
    private static final String TENANT_ID = "123";
    private static final String MANAGED_IDENTITY_CLIENT_ID = "456";
    private static final String LOCAL_STORAGE_ACCOUNT_NAME = "testAccount";
    private static final String LOCAL_BLOB_ENDPOINT = "http://127.0.0.1:10000/" + LOCAL_STORAGE_ACCOUNT_NAME;
    private static final String AZURE_STORAGE_ACCOUNT_NAME = "azureAccount";
    private static final String AZURE_STORAGE_ACCOUNT_KEY = "12345";
    private static final String AZURE_STORAGE_ACCOUNT_URL = "https://"
        + AZURE_STORAGE_ACCOUNT_NAME
        + ".blob.core.windows.net";
    private static final String AZURE_BLOB_ENDPOINT = "https://"
        + AZURE_STORAGE_ACCOUNT_NAME
        + ".blob.core.windows.net";
    private static final String CONNECTION_STRING = "DefaultEndpointsProtocol=http;AccountName="
        + LOCAL_STORAGE_ACCOUNT_NAME
        + "; AccountKey=123/456; BlobEndpoint="
        + LOCAL_BLOB_ENDPOINT;
    private static final String CONTAINER_NAME = "publications";

    @Mock
    private AzureBlobConfigurationProperties blobConfigProperties;

    @Mock
    private Environment env;

    @InjectMocks
    private AzureBlobConfiguration azureBlobConfiguration;

    @BeforeEach
    void setup() {
        when(env.getActiveProfiles()).thenReturn(new String[1]);
        ReflectionTestUtils.setField(azureBlobConfiguration, "tenantId", TENANT_ID);
        when(blobConfigProperties.getContainerName()).thenReturn(CONTAINER_NAME);
    }

    @Test
    void testCreationOfAzureBlobClientWithDevProfile() {
        when(env.getActiveProfiles()).thenReturn(BLOB_STORAGE_DEV_PROFILE);
        when(blobConfigProperties.getStorageAccountName()).thenReturn(AZURE_STORAGE_ACCOUNT_NAME);
        when(blobConfigProperties.getStorageAccountUrl()).thenReturn(AZURE_STORAGE_ACCOUNT_URL);
        when(blobConfigProperties.getStorageAccountKey()).thenReturn(AZURE_STORAGE_ACCOUNT_KEY);
        when(blobConfigProperties.getContainerName()).thenReturn(CONTAINER_NAME);

        BlobContainerClient blobContainerClient = azureBlobConfiguration.blobContainerClient(
            blobConfigProperties
        );

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(blobContainerClient).isNotNull();
        softly.assertThat(blobContainerClient.getAccountUrl()).isEqualTo(AZURE_STORAGE_ACCOUNT_URL);
        softly.assertThat(blobContainerClient.getAccountName()).isEqualTo(AZURE_STORAGE_ACCOUNT_NAME);
        softly.assertThat(blobContainerClient.getBlobContainerName()).isEqualTo(CONTAINER_NAME);

        softly.assertAll();
    }

    @Test
    void testCreationOfAzureBlobClientWithManagedIdentity() {
        ReflectionTestUtils.setField(azureBlobConfiguration, "managedIdentityClientId", MANAGED_IDENTITY_CLIENT_ID);
        when(env.getActiveProfiles()).thenReturn(new String[1]);
        when(blobConfigProperties.getStorageAccountName()).thenReturn(AZURE_STORAGE_ACCOUNT_NAME);

        BlobContainerClient blobContainerClient = azureBlobConfiguration.blobContainerClient(blobConfigProperties);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(blobContainerClient).isNotNull();
        softly.assertThat(blobContainerClient.getAccountUrl()).isEqualTo(AZURE_BLOB_ENDPOINT);
        softly.assertThat(blobContainerClient.getAccountName()).isEqualTo(AZURE_STORAGE_ACCOUNT_NAME);
        softly.assertThat(blobContainerClient.getBlobContainerName()).isEqualTo(CONTAINER_NAME);

        softly.assertAll();
    }

    @Test
    void testCreationOfAzureBlobClientWithoutManagedIdentity() {
        ReflectionTestUtils.setField(azureBlobConfiguration, "managedIdentityClientId", "");
        when(env.getActiveProfiles()).thenReturn(new String[1]);
        when(blobConfigProperties.getConnectionString()).thenReturn(CONNECTION_STRING);

        BlobContainerClient blobContainerClient = azureBlobConfiguration.blobContainerClient(blobConfigProperties);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(blobContainerClient).isNotNull();
        softly.assertThat(blobContainerClient.getAccountUrl()).isEqualTo(LOCAL_BLOB_ENDPOINT);
        softly.assertThat(blobContainerClient.getAccountName()).isEqualTo(LOCAL_STORAGE_ACCOUNT_NAME);
        softly.assertThat(blobContainerClient.getBlobContainerName()).isEqualTo(CONTAINER_NAME);

        softly.assertAll();
    }
}

