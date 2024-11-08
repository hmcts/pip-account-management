package uk.gov.hmcts.reform.pip.account.management.config;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.kiota.ApiException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("integration")
@Configuration
public class AzureConfigurationClientTestConfiguration {

    @Mock
    GraphServiceClient graphClientMock;

    @Mock
    UsersRequestBuilder usersRequestBuilderMock;

    @Mock
    ApiException apiExceptionMock;


    public AzureConfigurationClientTestConfiguration() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Creates the bean that is used to make requests to azure graph.
     * @return The azure graph client.
     */
    @Bean
    public GraphServiceClient graphClient() {
        return graphClientMock;
    }

    @Bean
    public UsersRequestBuilder usersRequestBuilder() {
        return usersRequestBuilderMock;
    }

    @Bean
    public ApiException apiException() {
        return apiExceptionMock;
    }

}
