package uk.gov.hmcts.reform.pip.account.management.service;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import nl.altindag.log.LogCaptor;
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
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;

import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.createApplicationList;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class})
@ActiveProfiles({"test", "non-async"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@SuppressWarnings({"PMD.TooManyMethods"})
class PublicationServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;

    private static final String SENT_MESSAGE = "test email sent";
    private static final String MESSAGES_MATCH = "Returned messages should match";
    private static final String EMAIL = "test@email.com";
    private static final String FULL_NAME = "FULL_NAME";
    private static final String LAST_SIGNED_IN_DATE = "15 July 2022";

    @Autowired
    PublicationService publicationService;

    LogCaptor logCaptor = LogCaptor.forClass(PublicationService.class);

    @BeforeEach
    void setup() throws IOException {
        mockPublicationServicesEndpoint = new MockWebServer();
        mockPublicationServicesEndpoint.start(8081);
    }

    @AfterEach
    void teardown() throws IOException {
        mockPublicationServicesEndpoint.shutdown();
    }

    @Test
    void testSendEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertTrue(publicationService.sendNotificationEmail(EMAIL,
                                                            "forename",
                                                             "surname"
        ), "No trigger sent");

        assertTrue(logCaptor.getInfoLogs().get(0).contains(SENT_MESSAGE), MESSAGES_MATCH);
    }

    @Test
    void testFailedEmailSend() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertFalse(publicationService.sendNotificationEmail(EMAIL,
                                                             "forename",
                                                             "surname"),
                     "trigger sent in error"
        );
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Request failed with error message:"),
                   "Error logs not being captured.");
    }

    @Test
    void testSendMediaNotificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertTrue(publicationService.sendMediaNotificationEmail(EMAIL, FULL_NAME, true),
                   "Should return true");
        assertTrue(logCaptor.getInfoLogs().get(0).contains(SENT_MESSAGE), MESSAGES_MATCH);
    }

    @Test
    void testSendMediaNotificationEmailFails() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertFalse(publicationService.sendMediaNotificationEmail(EMAIL, FULL_NAME, true),
                    "Should return false");
        assertTrue(logCaptor.getErrorLogs().get(0).contains(
            "Request to publication services /notify/welcome-email failed"), MESSAGES_MATCH);
    }

    @Test
    void testSendDuplicateMediaAccountEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertTrue(publicationService.sendNotificationEmailForDuplicateMediaAccount(
            EMAIL, FULL_NAME),
                     "No duplicate media account email sent");
        assertTrue(logCaptor.getInfoLogs().get(0).contains(SENT_MESSAGE), MESSAGES_MATCH);
    }

    @Test
    void testFailedDuplicateMediaAccountEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertFalse(publicationService.sendNotificationEmailForDuplicateMediaAccount(
            EMAIL, FULL_NAME), "Expected error message not in response");

        assertTrue(logCaptor.getErrorLogs().get(0).contains(
            "Request failed with error message"), MESSAGES_MATCH);
    }

    @Test
    void testSendMediaApplicationReportingEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertEquals(SENT_MESSAGE, publicationService.sendMediaApplicationReportingEmail(
            createApplicationList(2)),
                     "No application list sent");
    }

    @Test
    void testSendMediaAccountRejectionEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        MediaApplication mediaApplication = new MediaApplication();
        mediaApplication.setFullName("John Doe");
        mediaApplication.setId(UUID.randomUUID());
        mediaApplication.setEmail("john.doe@example.com");
        String reasons = "Rejection reasons go here";

        assertTrue(publicationService.sendMediaAccountRejectionEmail(mediaApplication, reasons),
                   "Failed to send media account rejection email");

        assertTrue(logCaptor.getInfoLogs().get(0).contains(SENT_MESSAGE), MESSAGES_MATCH);
    }


    @Test
    void testFailedMediaApplicationReportEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));
        String expectedResponse = String.format(
            "Email request failed to send with list of applications: %s",
            createApplicationList(2));

        assertTrue(publicationService.sendMediaApplicationReportingEmail(createApplicationList(2))
                       .contains(expectedResponse), "Expected error message not in response");
    }

    @Test
    void testSendAccountVerificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertEquals(SENT_MESSAGE, publicationService.sendAccountVerificationEmail(EMAIL, FULL_NAME),
                     "No user data sent");
    }

    @Test
    void testFailedAccountVerificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertTrue(publicationService.sendAccountVerificationEmail(EMAIL, FULL_NAME)
                       .contains("Media account verification email failed to send with error:"),
                   "No error was sent back");
    }

    @Test
    void testSendAccountSignInNotificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertEquals(SENT_MESSAGE, publicationService.sendInactiveAccountSignInNotificationEmail(EMAIL, FULL_NAME,
                                                                                                 UserProvenances.PI_AAD,
                                                                                                 LAST_SIGNED_IN_DATE),
                     "Notification email not sent");
    }

    @Test
    void testFailedAccountSignInNotificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertTrue(publicationService.sendInactiveAccountSignInNotificationEmail(EMAIL, FULL_NAME,
                                                                                 UserProvenances.PI_AAD,
                                                                                 LAST_SIGNED_IN_DATE)
                       .contains("Inactive user sign-in notification email failed to send with error:"),
                   "No error was sent back");
    }

    @Test
    void testSendSystemAdminAccountAction() {
        var testSystemAdminAction = new CreateSystemAdminAction();
        testSystemAdminAction.setAccountEmail("test@email.com");
        testSystemAdminAction.setActionResult(ActionResult.SUCCEEDED);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertEquals(SENT_MESSAGE, publicationService.sendSystemAdminAccountAction(testSystemAdminAction),
                     "Notification email not sent");
    }

    @Test
    void testFailedSendSystemAdminAccountAction() {
        var testSystemAdminAction = new CreateSystemAdminAction();
        testSystemAdminAction.setAccountEmail("test@email.com");
        testSystemAdminAction.setActionResult(ActionResult.FAILED);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertTrue(publicationService.sendSystemAdminAccountAction(testSystemAdminAction).contains(
            "Publishing of system admin account action failed with error:"), "No error was sent back");
    }
}
