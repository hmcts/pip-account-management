package uk.gov.hmcts.reform.pip.account.management.config;

import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.kiota.ApiException;
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
@Profile("functional")
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
