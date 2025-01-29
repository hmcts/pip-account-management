package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.core.http.ContentType;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ActiveProfiles({"test", "non-async"})
class DataManagementServiceTest {
    private static final String INVALID = "test";

    private final MockWebServer mockDataManagementEndpoint = new MockWebServer();
    private DataManagementService dataManagementService;

    @BeforeEach
    void setup() {
        WebClient mockedWebClient = WebClient.builder()
            .baseUrl(mockDataManagementEndpoint.url("/").toString())
            .build();
        dataManagementService = new DataManagementService(mockedWebClient);
    }

    @AfterEach
    void shutdown() throws IOException {
        mockDataManagementEndpoint.shutdown();
    }

    @Test
    void testGetCourt() {
        mockDataManagementEndpoint.enqueue(new MockResponse()
                                               .addHeader("Content-Type", ContentType.APPLICATION_JSON)
                                               .setBody("{\"name\": \"SJP Court\"}"));

        String courtName = dataManagementService.getCourtName("1");
        assertEquals("SJP Court", courtName, "Court name does not match returned value");
    }

    @Test
    void testNullCourt() {
        mockDataManagementEndpoint.enqueue(new MockResponse());

        String courtName = dataManagementService.getCourtName("1");
        assertNull(courtName, "Court return is null");
    }

    @Test
    void testGetCourtThrows() {
        mockDataManagementEndpoint.enqueue(new MockResponse().setResponseCode(404));
        String courtName = dataManagementService.getCourtName(INVALID);
        assertNull(courtName, "Court name not null when error occurred");
    }
}
