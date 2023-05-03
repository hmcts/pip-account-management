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
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.createApplication;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.createApplicationList;

@ExtendWith(MockitoExtension.class)
@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class})
@ActiveProfiles({"test", "non-async"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@SuppressWarnings({"PMD.TooManyMethods"})
class PublicationServiceTest {

    private static MockWebServer mockPublicationServicesEndpoint;

    private static final String SENT_MESSAGE = "test email sent";
    private static final String EMAIL = "test@email.com";
    private static final String FULL_NAME = "FULL_NAME";
    private static final String LAST_SIGNED_IN_DATE = "15 July 2022";
    private static final String ERROR_LOG_EMPTY_MESSAGE = "Error log is not empty";
    private static final String ERROR_LOG_MATCH_MESSAGE = "Error log does not match";

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

        assertTrue(publicationService.sendNotificationEmail(EMAIL, "forename", "surname"),
                   "No trigger sent");
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedEmailSend() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertFalse(publicationService.sendNotificationEmail(EMAIL, "forename", "surname"),
                    "trigger sent in error");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Admin account welcome email failed to send with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }

    @Test
    void testSendMediaNotificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertTrue(publicationService.sendMediaNotificationEmail(EMAIL, FULL_NAME, true),
                   "Should return true");
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testSendMediaNotificationEmailFails() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertFalse(publicationService.sendMediaNotificationEmail(EMAIL, FULL_NAME, true),
                    "Should return false");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Media account welcome email failed to send with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }

    @Test
    void testSendDuplicateMediaAccountEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        assertTrue(publicationService.sendNotificationEmailForDuplicateMediaAccount(EMAIL, FULL_NAME),
                   "No duplicate media account email sent");
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedDuplicateMediaAccountEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        assertFalse(publicationService.sendNotificationEmailForDuplicateMediaAccount(EMAIL, FULL_NAME),
                    "Expected error message not in response");
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Duplicate media account email failed to send with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }

    @Test
    void testSendMediaApplicationReportingEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        publicationService.sendMediaApplicationReportingEmail(createApplicationList(2));
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedMediaApplicationReportEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        publicationService.sendMediaApplicationReportingEmail(createApplicationList(2));
        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Media application reporting email failed to send with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }

    @Test
    void testSendMediaAccountRejectionEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        MediaApplication mediaApplication = new MediaApplication();
        mediaApplication.setFullName("John Doe");
        mediaApplication.setId(UUID.randomUUID());
        mediaApplication.setEmail("john.doe@example.com");

        Map<String, List<String>> reasons = new ConcurrentHashMap<>();
        reasons.put("Reason A", List.of("Text A", "Text B"));

        publicationService.sendMediaAccountRejectionEmail(mediaApplication, reasons);
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testSendMediaAccountRejectionEmailFailure() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(500));

        MediaApplication mediaApplication = createApplication(MediaApplicationStatus.REJECTED);

        Map<String, List<String>> reasons = new ConcurrentHashMap<>();
        reasons.put("Reason A", List.of("Text A", "Text B"));

        publicationService.sendMediaAccountRejectionEmail(mediaApplication, reasons);
        assertTrue(logCaptor.getErrorLogs().get(0).contains("Media account rejection email failed to send with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }

    @Test
    void testSendAccountVerificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        publicationService.sendAccountVerificationEmail(EMAIL, FULL_NAME);
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedAccountVerificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        publicationService.sendAccountVerificationEmail(EMAIL, FULL_NAME);
        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Media account verification email failed to send with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }

    @Test
    void testSendAccountSignInNotificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        publicationService.sendInactiveAccountSignInNotificationEmail(EMAIL, FULL_NAME, UserProvenances.PI_AAD,
                                                                      LAST_SIGNED_IN_DATE);
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedAccountSignInNotificationEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        publicationService.sendInactiveAccountSignInNotificationEmail(EMAIL, FULL_NAME, UserProvenances.PI_AAD,
                                                                      LAST_SIGNED_IN_DATE);
        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Inactive user sign-in notification email failed to send with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }

    @Test
    void testSendSystemAdminAccountAction() {
        CreateSystemAdminAction testSystemAdminAction = new CreateSystemAdminAction();
        testSystemAdminAction.setAccountEmail("test@email.com");
        testSystemAdminAction.setActionResult(ActionResult.SUCCEEDED);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SENT_MESSAGE));

        publicationService.sendSystemAdminAccountAction(testSystemAdminAction);
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedSendSystemAdminAccountAction() {
        CreateSystemAdminAction testSystemAdminAction = new CreateSystemAdminAction();
        testSystemAdminAction.setAccountEmail("test@email.com");
        testSystemAdminAction.setActionResult(ActionResult.FAILED);
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(400));

        publicationService.sendSystemAdminAccountAction(testSystemAdminAction);
        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Publishing of system admin account action failed with error:"),
                   ERROR_LOG_MATCH_MESSAGE);
    }
}
