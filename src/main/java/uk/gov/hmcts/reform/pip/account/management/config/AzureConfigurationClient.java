package uk.gov.hmcts.reform.pip.account.management.config;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration class used to initialise beans to talk to Azure graph.
 */
@Profile("!test & !functional")
@Configuration
public class AzureConfigurationClient {

    /**
     * Creates the bean that is used to make requests to azure graph.
     * @return The azure graph client.
     */
    @Bean
    public GraphServiceClient graphClient(ClientConfiguration clientConfiguration) {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(clientConfiguration.getClientId())
            .clientSecret(clientConfiguration.getClientSecret())
            .tenantId(clientConfiguration.getTenantGuid())
            .build();

        return new GraphServiceClient(clientSecretCredential, clientConfiguration.getTokenProvider());
    }

}
