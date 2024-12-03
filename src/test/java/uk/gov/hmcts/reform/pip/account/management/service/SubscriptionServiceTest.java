package uk.gov.hmcts.reform.pip.account.management.service;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test", "non-async"})
class SubscriptionServiceTest {

    private final MockWebServer mockSubscriptionManagementEndpoint = new MockWebServer();

    SubscriptionService subscriptionService;

    private static final String TEST_RESPONSE_STRING = "All subscriptions for user deleted";

    @BeforeEach
    void setup() throws IOException {
        mockSubscriptionManagementEndpoint.start(4550);

        WebClient mockedWebClient =
            WebClient.builder()
                     .baseUrl(mockSubscriptionManagementEndpoint.url("/").toString())
                     .build();

        subscriptionService = new SubscriptionService(mockedWebClient);
    }

    @AfterEach
    void teardown() throws IOException {
        mockSubscriptionManagementEndpoint.shutdown();
    }

    @Test
    void testSendSubscriptionDeletionRequest() {
        mockSubscriptionManagementEndpoint.enqueue(new MockResponse().setBody(TEST_RESPONSE_STRING));

        assertEquals(TEST_RESPONSE_STRING, subscriptionService.sendSubscriptionDeletionRequest("1234"),
                     "No request sent"
        );
    }

    @Test
    void testSendSubscriptionDeletionRequestFailed() {
        mockSubscriptionManagementEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertTrue(subscriptionService.sendSubscriptionDeletionRequest("1234")
                       .contains("Deletion request to subscription management failed with error"),
                   "No error message sent back");
    }
}
