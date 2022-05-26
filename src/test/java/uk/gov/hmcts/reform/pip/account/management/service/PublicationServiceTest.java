package uk.gov.hmcts.reform.pip.account.management.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.createApplicationList;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {AzureConfigurationClientTest.class, Application.class})
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class PublicationServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;

    @Autowired
    PublicationService publicationService;

    LogCaptor logCaptor = LogCaptor.forClass(PublicationService.class);

    private static final String TEST_EMAIL_SENT = "test email sent";


    @Test
    void testSendEmail() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(TEST_EMAIL_SENT));

        assertEquals(TEST_EMAIL_SENT, publicationService.sendNotificationEmail("test@example.com",
                                                                                 "forename", "surname"
        ), "No trigger sent");
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testFailedEmailSend() {
        assertEquals("Email request failed to send: test@example.com",
                     publicationService.sendNotificationEmail("test@example.com", "forename", "surname"),
                     "trigger sent in error"
        );
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Request failed with error message:"),
                   "Error logs not being captured.");
    }

    @Test
    void testSendMediaApplicationReportingEmail() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(TEST_EMAIL_SENT));

        assertEquals(TEST_EMAIL_SENT, publicationService.sendMediaApplicationReportingEmail(
            createApplicationList(2)),
                     "No trigger sent");
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testFailedMediaApplicationReportEmail() {
        String expectedResponse = String.format(
            "Email request failed to send with list of applications: %s",
            createApplicationList(2));

        assertEquals(expectedResponse, publicationService.sendMediaApplicationReportingEmail(
            createApplicationList(2)),
                     "trigger sent in error");

        assertTrue(logCaptor.getErrorLogs().get(0).contains("With error message:"),
                   "Error logs not being captured.");
    }

}

