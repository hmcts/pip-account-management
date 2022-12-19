package uk.gov.hmcts.reform.pip.account.management.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class})
@ActiveProfiles({"test", "non-async"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class SubscriptionServiceTest {

    private static MockWebServer mockSubscriptionManagementEndpoint;

    @Autowired
    SubscriptionService subscriptionService;

    private static final String TEST_RESPONSE_STRING = "All subscriptions for user deleted";

    @BeforeEach
    void setup() throws IOException {
        mockSubscriptionManagementEndpoint = new MockWebServer();
        mockSubscriptionManagementEndpoint.start(4550);
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
