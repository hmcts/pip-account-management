package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionLocationService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.UserSubscriptionService;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class})
class SubscriptionControllerTest {

    private Subscription mockSubscription;
    private List<Subscription> mockSubscriptionList;

    private static final String SEARCH_VALUE = "193254";
    private static final String STATUS_CODE_MATCH = "Status codes should match";
    private static final String RETURNED_SUBSCRIPTION_NOT_MATCHED =
        "Returned subscription does not match expected subscription";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final List<String> LIST_TYPES = Arrays.asList(ListType.CIVIL_DAILY_CAUSE_LIST.name());
    private static final List<String> LIST_LANGUAGE = Arrays.asList("ENGLISH");
    private static final String LOCATION_ID = "1";
    private static final String ACTIONING_USER_ID = "1234-1234";
    private static final String REQUESTER_NAME = "ReqName";
    private static final String USER_ID = UUID.randomUUID().toString();

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    UserSubscriptionService userSubscriptionService;

    @Mock
    SubscriptionNotificationService subscriptionNotificationService;

    @Mock
    SubscriptionLocationService subscriptionLocationService;

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

        uk.gov.hmcts.reform.pip.model.subscription.Subscription modelSubscription = mockSubscription.toDto();
        modelSubscription.setCreatedDate(modelSubscription.getCreatedDate());

        assertEquals(
            new ResponseEntity<>(
                String.format("Subscription created with the id %s for user %s",
                              mockSubscription.getId(), mockSubscription.getUserId()
                ),
                HttpStatus.CREATED
            ),
            subscriptionController.createSubscription(modelSubscription, ACTIONING_USER_ID),
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
            subscriptionController.createSubscription(mockSubscription.toDto(), ACTIONING_USER_ID),
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
        ResponseEntity<String> response = subscriptionController.bulkDeleteSubscriptionsV2(testIds, ACTIONING_USER_ID);

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
    void testMiDataReturnsOk() {
        assertEquals(
            HttpStatus.OK,
            subscriptionController.getSubscriptionDataForMiReportingLocal().getStatusCode(),
            STATUS_CODE_MATCH
        );
        assertEquals(
            HttpStatus.OK,
            subscriptionController.getSubscriptionDataForMiReportingAll().getStatusCode(),
            STATUS_CODE_MATCH
        );
    }

    @Test
    void testAddListTypesForSubscription() {
        doNothing().when(subscriptionService).addListTypesForSubscription(subscriptionListType, USER_ID);

        assertEquals(
            new ResponseEntity<>(
                String.format(
                    "Location list Type successfully added for user %s",
                    USER_ID
                ),
                HttpStatus.CREATED
            ),
            subscriptionController.addListTypesForSubscription(USER_ID, subscriptionListType),
                RETURNED_SUBSCRIPTION_NOT_MATCHED
        );
    }

    @Test
    void testConfigureListTypesForSubscription() {
        doNothing().when(subscriptionService).configureListTypesForSubscription(subscriptionListType, USER_ID);

        assertEquals(
            new ResponseEntity<>(
                String.format(
                    "Location list Type successfully updated for user %s",
                    USER_ID
                ),
                HttpStatus.OK
            ),
            subscriptionController.configureListTypesForSubscription(USER_ID, subscriptionListType),
                RETURNED_SUBSCRIPTION_NOT_MATCHED
        );
    }

    @Test
    void testDeleteSubscriptionsByUserId() {
        when(userSubscriptionService.deleteAllByUserId("test string")).thenReturn(
            "All subscriptions deleted for user id");
        assertEquals(
            "All subscriptions deleted for user id",
            subscriptionController.deleteAllSubscriptionsForUser("test string").getBody(),
            "Subscription for user should be deleted"
        );
    }

    @Test
    void testFindSubscriptionsByLocationId() {
        when(subscriptionLocationService.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionController.findSubscriptionsByLocationId(LOCATION_ID).getBody(),
                     "The found subscription does not match expected subscription");
    }

    @Test
    void testFindSubscriptionsByLocationIdReturnsOk() {
        when(subscriptionLocationService.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        assertEquals(HttpStatus.OK, subscriptionController.findSubscriptionsByLocationId(LOCATION_ID)
                         .getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testDeleteSubscriptionByLocationReturnsOk() throws JsonProcessingException {
        when(subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, USER_ID))
            .thenReturn("Total 10 subscriptions deleted for location id");

        assertEquals(HttpStatus.OK, subscriptionController.deleteSubscriptionByLocation(
            USER_ID, Integer.parseInt(LOCATION_ID)).getStatusCode(),
                     "Delete subscription location endpoint has not returned OK");
    }
}
