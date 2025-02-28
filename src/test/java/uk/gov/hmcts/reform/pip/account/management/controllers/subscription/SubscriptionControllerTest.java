package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.UserSubscription;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.UserSubscriptionService;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    private Subscription mockSubscription;
    private List<Subscription> mockSubscriptionList;

    private static final String SEARCH_VALUE = "193254";
    private static final String STATUS_CODE_MATCH = "Status codes should match";
    private static final String RETURNED_SUBSCRIPTION_NOT_MATCHED =
        "Returned subscription does not match expected subscription";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final List<String> LIST_TYPES = List.of(ListType.CIVIL_DAILY_CAUSE_LIST.name());
    private static final List<String> LIST_LANGUAGE = List.of("ENGLISH");
    private static final String ACTIONING_USER_ID = "1234-1234";
    private static final String USER_ID = UUID.randomUUID().toString();

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    UserSubscriptionService userSubscriptionService;

    @Mock
    SubscriptionNotificationService subscriptionNotificationService;

    @InjectMocks
    SubscriptionController subscriptionController;

    UserSubscription userSubscription;

    SubscriptionListType subscriptionListType;

    @BeforeEach
    void setup() {
        mockSubscription = SubscriptionUtils.createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, LocalDateTime.now());
        userSubscription = new UserSubscription();
        subscriptionListType = new SubscriptionListType(USER_ID, LIST_TYPES, LIST_LANGUAGE);
    }

    @Test
    void testLocationSubscription() {
        when(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID))
            .thenReturn(mockSubscription);

        assertEquals(
            new ResponseEntity<>(
                String.format("Subscription created with the id %s for user %s",
                              mockSubscription.getId(), mockSubscription.getUserId()
                ),
                HttpStatus.CREATED
            ),
            subscriptionController.createSubscription(mockSubscription, ACTIONING_USER_ID),
                RETURNED_SUBSCRIPTION_NOT_MATCHED
        );
    }

    @Test
    void testCreateCaseSubscription() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID))
            .thenReturn(mockSubscription);

        assertEquals(
            new ResponseEntity<>(
                String.format("Subscription created with the id %s for user %s",
                              mockSubscription.getId(), mockSubscription.getUserId()
                ),
                HttpStatus.CREATED
            ),
            subscriptionController.createSubscription(mockSubscription, ACTIONING_USER_ID),
                RETURNED_SUBSCRIPTION_NOT_MATCHED
        );
    }

    @Test
    void testDeleteSubscription() {
        UUID testUuid = UUID.randomUUID();
        doNothing().when(subscriptionService).deleteById(testUuid, ACTIONING_USER_ID);
        assertEquals(String.format(
            "Subscription: %s was deleted", testUuid), subscriptionController.deleteById(
                testUuid, ACTIONING_USER_ID).getBody(), "Subscription should be deleted"
        );
    }

    @Test
    void testDeleteSubscriptionReturnsOk() {
        UUID testUuid = UUID.randomUUID();
        doNothing().when(subscriptionService).deleteById(testUuid, ACTIONING_USER_ID);
        assertEquals(HttpStatus.OK, subscriptionController.deleteById(testUuid, ACTIONING_USER_ID).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testBulkDeleteSubscriptionsV2Success() {
        UUID testId1 = UUID.randomUUID();
        UUID testId2 = UUID.randomUUID();
        UUID testId3 = UUID.randomUUID();
        List<UUID> testIds = List.of(testId1, testId2, testId3);
        String expectedTestIds = String.join(", ", new String[]{
            testId1.toString(),
            testId2.toString(),
            testId3.toString()
        });

        doNothing().when(subscriptionService).bulkDeleteSubscriptions(testIds);
        ResponseEntity<String> response = subscriptionController.bulkDeleteSubscriptions(testIds, ACTIONING_USER_ID);

        assertEquals(String.format("Subscriptions with ID %s deleted", expectedTestIds), response.getBody(),
                     "Subscription should be deleted");
        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testFindSubscription() {
        when(subscriptionService.findById(any())).thenReturn(mockSubscription);
        assertEquals(mockSubscription, subscriptionController.findBySubId(UUID.randomUUID()).getBody(), "The found "
            + "subscription does not match expected subscription");
    }

    @Test
    void testFindSubscriptionReturnsOk() {
        when(subscriptionService.findById(any())).thenReturn(mockSubscription);
        assertEquals(HttpStatus.OK, subscriptionController.findBySubId(UUID.randomUUID()).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testFindByUserId() {
        when(userSubscriptionService.findByUserId(USER_ID)).thenReturn(userSubscription);
        assertEquals(userSubscription, subscriptionController.findByUserId(USER_ID).getBody(),
                     "Should return users subscriptions"
        );
    }

    @Test
    void testFindByUserIdReturnsOk() {
        when(userSubscriptionService.findByUserId(USER_ID)).thenReturn(userSubscription);
        assertEquals(HttpStatus.OK, subscriptionController.findByUserId(USER_ID).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testArtefactRecipientsReturnsAccepted() {
        doNothing().when(subscriptionNotificationService).collectSubscribers(any());
        assertEquals(HttpStatus.ACCEPTED, subscriptionController.buildSubscriberList(new Artefact()).getStatusCode(),
                     STATUS_CODE_MATCH
        );
    }

    @Test
    void testBuildDeletedArtefactSubscribers() {
        doNothing().when(subscriptionNotificationService).collectThirdPartyForDeletion(any());
        assertEquals(HttpStatus.ACCEPTED, subscriptionController.buildDeletedArtefactSubscribers(new Artefact())
            .getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testMiDataAllSubscriptionsReturnsSuccessfully() {
        AllSubscriptionMiData allSubscriptionMiData = new AllSubscriptionMiData();
        allSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionService.getAllSubscriptionsDataForMiReportingV2()).thenReturn(
            List.of(allSubscriptionMiData)
        );

        ResponseEntity<List<AllSubscriptionMiData>> response =
            subscriptionController.getSubscriptionDataForMiReportingAllV2();

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertTrue(response.getBody().contains(allSubscriptionMiData),
                   RETURNED_SUBSCRIPTION_NOT_MATCHED);
    }

    @Test
    void testMiDataLocationSubscriptionsReturnsSuccessfully() {
        LocationSubscriptionMiData locationSubscriptionMiData = new LocationSubscriptionMiData();
        locationSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionService.getLocationSubscriptionsDataForMiReportingV2()).thenReturn(
            List.of(locationSubscriptionMiData)
        );

        ResponseEntity<List<LocationSubscriptionMiData>> response =
            subscriptionController.getSubscriptionDataForMiReportingLocationV2();

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertTrue(response.getBody().contains(locationSubscriptionMiData),
                   RETURNED_SUBSCRIPTION_NOT_MATCHED);
    }
}
