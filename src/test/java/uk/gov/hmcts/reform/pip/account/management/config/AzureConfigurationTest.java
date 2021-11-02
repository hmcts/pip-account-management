package uk.gov.hmcts.reform.pip.account.management.config;

import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureConfigurationTest {

    @Mock
    ClientConfiguration clientConfiguration;

    @Mock
    TableConfiguration tableConfiguration;

    @InjectMocks
    AzureConfigurationClient azureConfigurationClient;

    @Test
    void testGraphClientCreation() {
        when(clientConfiguration.getClientId()).thenReturn("1234");
        when(clientConfiguration.getClientSecret()).thenReturn("12345");
        when(clientConfiguration.getTenantGuid()).thenReturn("12345");
        when(clientConfiguration.getTokenProvider()).thenReturn("12345");

        GraphServiceClient<Request> graphServiceClient = azureConfigurationClient.graphClient();

        assertNotNull(graphServiceClient, "Azure Graph Service client has been created");
    }

    @Test
    void testTableClientInvalidConnectionString() {
        when(tableConfiguration.getConnectionString()).thenReturn("1234");

        assertThrows(IllegalArgumentException.class, () -> {
            azureConfigurationClient.tableClient();
        }, "IllegalArgumentException is thrown when invalid argument is provided");
    }

}
