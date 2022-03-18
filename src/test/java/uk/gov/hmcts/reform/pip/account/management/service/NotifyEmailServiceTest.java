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
import uk.gov.hmcts.reform.pip.account.management.Application;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {Application.class})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class NotifyEmailServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;

    @Autowired
    NotifyEmailService notifyEmailService;

    LogCaptor logCaptor = LogCaptor.forClass(NotifyEmailService.class);


    @Test
    void testSendEmail() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody("test email sent"));

        assertEquals("test email sent", notifyEmailService.sendNotificationEmail("test@example.com"), "No trigger "
            + "sent");
        mockPublicationServicesEndpoint.shutdown();
        assertTrue(logCaptor.getInfoLogs().get(1).contains(String.format("Email trigger for %s sent to "
            + "Publication-Services", "test@example.com")), "No trigger sent");
    }

    @Test
    void testFailedEmailSend() {
        assertEquals("Email request failed to send: test@example.com",
                     notifyEmailService.sendNotificationEmail("test@example.com"), "trigger sent in error");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Request failed with error message:"), "Error logs not "
            + "being captured.");
    }
}

