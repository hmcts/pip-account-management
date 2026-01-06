package uk.gov.hmcts.reform.pip.account.management.service;

import com.azure.core.http.ContentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.altindag.log.LogCaptor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.BulkSubscriptionsSummary;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionsSummaryDetails;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyAction;
import uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartyOauthConfiguration;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.reform.pip.account.management.helpers.MediaApplicationHelper.createApplication;
import static uk.gov.hmcts.reform.pip.account.management.helpers.MediaApplicationHelper.createApplicationList;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles({"test", "non-async"})
class PublicationServiceTest {

    private final MockWebServer mockPublicationServicesEndpoint = new MockWebServer();

    private static final String SENT_MESSAGE = "test email sent";
    private static final String EMAIL = "test@email.com";
    private static final String FULL_NAME = "FULL_NAME";
    private static final String LAST_SIGNED_IN_DATE = "15 July 2022";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String TEST_ID = "123";
    private static final UUID ARTEFACT_ID = UUID.randomUUID();

    private static final String TEST_API_DESTINATION = "http://www.abc.com";
    private static final String SUCCESSFULLY_SENT = "Successfully sent";
    private static final Artefact TEST_ARTEFACT = new Artefact();

    private static final String ERROR_LOG_EMPTY_MESSAGE = "Error log is not empty";
    private static final String ERROR_LOG_MATCH_MESSAGE = "Error log does not match";

    private PublicationService publicationService;

    private final LogCaptor logCaptor = LogCaptor.forClass(PublicationService.class);
    private final SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
    private final Subscription subscription = new Subscription();

    @BeforeEach
    void setup() throws IOException {
        subscriptionsSummary.setEmail(EMAIL);
        subscription.setSearchType(SearchType.CASE_ID);
        subscription.setSearchValue(TEST_ID);

        mockPublicationServicesEndpoint.start(8081);

        WebClient mockedWebClient =
            WebClient.builder()
                     .baseUrl(mockPublicationServicesEndpoint.url("/").toString())
                     .build();

        publicationService = new PublicationService(mockedWebClient);
    }

    @AfterEach
    void teardown() throws IOException {
        mockPublicationServicesEndpoint.shutdown();
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

    @Test
    void testPostSubscriptionSummariesRequestUrl() throws InterruptedException {
        subscription.setSearchType(SearchType.LIST_TYPE);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);

        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();
        assertNotNull(recordedRequest.getRequestUrl(), "Request URL should not be null");
        assertTrue(recordedRequest.getRequestUrl().toString().contains("/notify/subscription"),
                   "Request URL should be correct");
    }

    @Test
    void testPostSubscriptionSummariesRequestBodyEmail() throws IOException, InterruptedException {
        subscription.setSearchType(SearchType.LIST_TYPE);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);
        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        SubscriptionsSummary subscriptionsSummary = bulkSubscriptionsSummary.getSubscriptionEmails().get(0);
        assertEquals(EMAIL, subscriptionsSummary.getEmail(), "Subscription email should match");
    }

    @Test
    void testPostSubscriptionSummariesRequestBodyArtefactId() throws IOException, InterruptedException {
        subscription.setSearchType(SearchType.LIST_TYPE);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);
        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        assertEquals(ARTEFACT_ID, bulkSubscriptionsSummary.getArtefactId(), "Subscription artefact ID should match");
    }

    @ParameterizedTest
    @EnumSource(value = SearchType.class, names = {"LOCATION_ID", "CASE_URN", "CASE_ID"})
    void testPostSubscriptionDifferentTypes(SearchType searchType)
        throws InterruptedException, IOException {
        subscription.setSearchType(searchType);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);

        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        SubscriptionsSummaryDetails subscriptionsSummaryDetailsReturned = bulkSubscriptionsSummary
            .getSubscriptionEmails().get(0).getSubscriptions();

        switch (searchType) {
            case LOCATION_ID -> {
                assertEquals(1, subscriptionsSummaryDetailsReturned.getLocationId().size(),
                             "Size of location IDs should match");
                assertEquals(TEST_ID, subscriptionsSummaryDetailsReturned.getLocationId().get(0),
                             "Location ID should match");
            }
            case CASE_URN -> {
                assertEquals(1, subscriptionsSummaryDetailsReturned.getCaseUrn().size(),
                             "Size of case URNs should match");
                assertEquals(TEST_ID, subscriptionsSummaryDetailsReturned.getCaseUrn().get(0),
                             "Case URN should match");
            }
            case CASE_ID -> {
                assertEquals(1, subscriptionsSummaryDetailsReturned.getCaseNumber().size(),
                             "Size of case numbers should match");
                assertEquals(TEST_ID, subscriptionsSummaryDetailsReturned.getCaseNumber().get(0),
                             "Case number should match");
            }
            default -> fail("Invalid search type");
        }
    }

    @Test
    void testPostSubscriptionSummariesWhenMultipleSubscriptions() throws InterruptedException, IOException {
        subscription.setSearchType(SearchType.LOCATION_ID);
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));
        subscriptionsMap.put("OtherTestEmail", List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE,
                                                               ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.postSubscriptionSummaries(ARTEFACT_ID, subscriptionsMap);

        RecordedRequest recordedRequest = mockPublicationServicesEndpoint.takeRequest();

        ObjectMapper objectMapper = new ObjectMapper();
        BulkSubscriptionsSummary bulkSubscriptionsSummary =
            objectMapper.readValue(recordedRequest.getBody().readByteArray(), BulkSubscriptionsSummary.class);

        assertEquals(2, bulkSubscriptionsSummary.getSubscriptionEmails().size(),
                     "Number of subscriptions should match when there are multiple subscriptions");
    }

    @Test
    void testPostSubscriptionSummariesThrows() {
        Map<String, List<Subscription>> subscriptionsMap = new ConcurrentHashMap<>();
        subscriptionsMap.put(EMAIL, List.of(subscription));

        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        publicationService.postSubscriptionSummaries(ARTEFACT_ID,
                                                             subscriptionsMap);

        assertTrue(
            logCaptor.getErrorLogs().get(0).contains("Subscription email failed to send with error"),
            ERROR_LOG_MATCH_MESSAGE
        );
    }

    @Test
    void testSendThirdPartyList() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.sendThirdPartyList(
            new ThirdPartySubscription(TEST_API_DESTINATION, UUID.randomUUID())
        );
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testSendEmptyArtefact() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.sendEmptyArtefact(
            new ThirdPartySubscriptionArtefact(TEST_API_DESTINATION, TEST_ARTEFACT)
        );
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testSendThirdPartySubscription() {
        ThirdPartyOauthConfiguration thirdPartyOauthConfiguration = new ThirdPartyOauthConfiguration(
            TEST_API_DESTINATION,
            "http://token.url",
            "clientIdKey",
            "clientSecretKey",
            "scopeKey"
        );
        uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription thirdPartySubscription =
            new uk.gov.hmcts.reform.pip.model.thirdparty.ThirdPartySubscription(
                List.of(thirdPartyOauthConfiguration), UUID.randomUUID(), ThirdPartyAction.NEW_PUBLICATION
            );

        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .addHeader(CONTENT_TYPE, ContentType.APPLICATION_JSON)
                                                    .setResponseCode(200));

        publicationService.sendThirdPartySubscription(thirdPartySubscription);
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testSendEmptyArtefactReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(404));

        publicationService.sendEmptyArtefact(
            new ThirdPartySubscriptionArtefact(TEST_API_DESTINATION, TEST_ARTEFACT)
        );

        assertTrue(logCaptor.getErrorLogs().get(0)
                       .contains("Deleted artefact notification to third party failed to send with error"),
                   ERROR_LOG_MATCH_MESSAGE
        );
    }

    @Test
    void testSendThirdPartyListReturnsFailed() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setResponseCode(404));

        publicationService.sendThirdPartyList(
            new ThirdPartySubscription(TEST_API_DESTINATION, UUID.randomUUID())
        );
        assertTrue(
            logCaptor.getErrorLogs().get(0).contains("Publication to third party failed to send with error"),
            ERROR_LOG_MATCH_MESSAGE
        );

    }

    @Test
    void testSendSystemAdminEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse().setBody(SUCCESSFULLY_SENT));

        publicationService.sendSystemAdminEmail(List.of("test@test.com"), EMAIL,
                                                        ActionResult.ATTEMPTED, "Error");
        assertTrue(logCaptor.getErrorLogs().isEmpty(), ERROR_LOG_EMPTY_MESSAGE);
    }

    @Test
    void testFailedSendSystemAdminEmail() {
        mockPublicationServicesEndpoint.enqueue(new MockResponse()
                                                    .setResponseCode(400));

        publicationService.sendSystemAdminEmail(List.of("test@test.com"), EMAIL,
                                                        ActionResult.ATTEMPTED, "Error");

        assertTrue(
            logCaptor.getErrorLogs().get(0).contains("System admin notification email failed to send with error"),
            ERROR_LOG_MATCH_MESSAGE
        );
    }
}
