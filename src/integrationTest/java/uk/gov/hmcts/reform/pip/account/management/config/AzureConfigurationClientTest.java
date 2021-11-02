package uk.gov.hmcts.reform.pip.account.management.config;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import okhttp3.Request;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class AzureConfigurationClientTest {

    @Mock
    GraphServiceClient<Request> graphClientMock;

    @Mock
    UserCollectionRequestBuilder userCollectionRequestBuilderMock;

    @Mock
    UserCollectionRequest userCollectionRequestMock;

    @Mock
    GraphServiceException graphServiceExceptionMock;

    @Mock
    TableClient tableClientMock;

    @Mock
    PagedIterable<TableEntity> tableEntitiesMock;

    public AzureConfigurationClientTest() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Creates the bean that is used to make requests to azure graph.
     * @return The azure graph client.
     */
    @Bean
    public GraphServiceClient<Request> graphClient() {
        return graphClientMock;
    }

    @Bean
    public TableClient tableClient() {
        return tableClientMock;
    }

    @Bean
    public UserCollectionRequestBuilder userCollectionRequestBuilder() {
        return userCollectionRequestBuilderMock;
    }

    @Bean
    public UserCollectionRequest userCollectionRequest() {
        return userCollectionRequestMock;
    }

    @Bean
    public GraphServiceException graphServiceException() {
        return graphServiceExceptionMock;
    }

    @Bean
    public PagedIterable<TableEntity> tableEntities() {
        return tableEntitiesMock;
    }



}
