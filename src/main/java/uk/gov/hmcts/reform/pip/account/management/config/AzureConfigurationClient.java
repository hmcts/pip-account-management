package uk.gov.hmcts.reform.pip.account.management.config;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.List;

/**
 * Configuration class used to initialise beans to talk to Azure graph.
 */
@Profile("!test")
@Configuration
public class AzureConfigurationClient {

    @Autowired
    ClientConfiguration clientConfiguration;

    @Autowired
    TableConfiguration tableConfiguration;

    /**
     * Creates the bean that is used to make requests to azure graph.
     * @return The azure graph client.
     */
    @Bean
    public GraphServiceClient<Request> graphClient() {
        ClientSecretCredential clientSecretCredential = new ClientSecretCredentialBuilder()
            .clientId(clientConfiguration.getClientId())
            .clientSecret(clientConfiguration.getClientSecret())
            .tenantId(clientConfiguration.getTenantGuid())
            .build();

        TokenCredentialAuthProvider tokenCredentialAuthProvider = new TokenCredentialAuthProvider(List.of(
            clientConfiguration.getTokenProvider()), clientSecretCredential);

        return
            GraphServiceClient
                .builder()
                .authenticationProvider(tokenCredentialAuthProvider)
                .buildClient();
    }

    @Bean
    public TableClient tableClient() {
        return new TableClientBuilder()
            .connectionString(tableConfiguration.getConnectionString())
            .tableName(tableConfiguration.getTableName())
            .buildClient();
    }
}
