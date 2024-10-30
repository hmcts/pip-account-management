package uk.gov.hmcts.reform.pip.account.management.config;

import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.common.StorageSharedKeyCredential;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
@Profile("!test & !integration & !functional")
public class AzureBlobConfiguration {
    private static final String BLOB_ENDPOINT = "https://%s.blob.core.windows.net/";
    private static final String DEV_PROFILE = "blobStorageDev";

    @Autowired
    private Environment env;

    @Value("${spring.cloud.azure.active-directory.profile.tenant-id}")
    private String tenantId;

    @Value("${azure.managed-identity.client-id}")
    private String managedIdentityClientId;

    @Bean
    public BlobContainerClient blobContainerClient(AzureBlobConfigurationProperties
                                                   configurationProperties) {
        if (Arrays.stream(env.getActiveProfiles())
            .anyMatch(DEV_PROFILE::equals)) {
            StorageSharedKeyCredential storageCredential = new StorageSharedKeyCredential(
                configurationProperties.getStorageAccountName(),
                configurationProperties.getStorageAccountKey()
            );

            return new BlobContainerClientBuilder()
                .endpoint(configurationProperties.getStorageAccountUrl())
                .credential(storageCredential)
                .containerName(configurationProperties.getContainerName())
                .buildClient();
        } else if (managedIdentityClientId.isEmpty()) {
            return new BlobContainerClientBuilder()
                .connectionString(configurationProperties.getConnectionString())
                .containerName(configurationProperties.getContainerName())
                .buildClient();
        }

        DefaultAzureCredential defaultCredential = new DefaultAzureCredentialBuilder()
            .tenantId(tenantId)
            .managedIdentityClientId(managedIdentityClientId)
            .build();

        return new BlobContainerClientBuilder()
            .endpoint(String.format(BLOB_ENDPOINT, configurationProperties.getStorageAccountName()))
            .credential(defaultCredential)
            .containerName(configurationProperties.getContainerName())
            .buildClient();
    }

}
