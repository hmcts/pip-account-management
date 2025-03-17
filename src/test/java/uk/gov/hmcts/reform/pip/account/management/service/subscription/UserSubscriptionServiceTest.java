package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.CaseSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.ListTypeSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.LocationSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.UserSubscription;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.createMockSubscription;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.createMockSubscriptionList;

@ExtendWith({MockitoExtension.class})
class UserSubscriptionServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID USER_ID_NO_SUBS = UUID.randomUUID();
    private static final String SEARCH_VALUE = "193254";
    private static final String CASE_ID = "123";

    private static final String PARTY_NAMES = "Party A, Party B";
    private static final String URN = "312";
    private static final String CASE_NAME = "case-name";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final String COURT_NAME = "test court name";
    private static final String LIST_NAME = ListType.CIVIL_DAILY_CAUSE_LIST.name();

    private static final List<String> LIST_TYPES = List.of(ListType.CIVIL_DAILY_CAUSE_LIST.name());
    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;

    private SubscriptionListType mockSubscriptionListType;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionListTypeRepository subscriptionListTypeRepository;

    @InjectMocks
    UserSubscriptionService userSubscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, DATE_ADDED);
        mockSubscriptionList = createMockSubscriptionList(DATE_ADDED);
        mockSubscription.setChannel(Channel.EMAIL);
        mockSubscriptionListType = new SubscriptionListType();
        mockSubscriptionListType.setListType(LIST_TYPES);
    }

    @Test
    void testNoSubscriptionsReturnsEmpty() {
        assertEquals(new UserSubscription(), userSubscriptionService.findByUserId(USER_ID_NO_SUBS),
                     "Should return empty user subscriptions");
    }

    @Test
    void testFindByUserIdOnlyCourt() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setLocationName("Test court");
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));
        when(subscriptionListTypeRepository
                 .findByUserId(any()))
            .thenReturn(Optional.of(mockSubscriptionListType));
        LocationSubscription expected = new LocationSubscription();
        expected.setSubscriptionId(mockSubscription.getId());
        expected.setLocationName("Test court");
        expected.setLocationId("193254");
        expected.setListType(List.of(LIST_NAME));
        expected.setDateAdded(DATE_ADDED);

        UserSubscription result = userSubscriptionService.findByUserId(USER_ID);

        assertEquals(List.of(expected), result.getLocationSubscriptions(),
                     "Should return court name");
        assertEquals(0, result.getCaseSubscriptions().size(), "Cases should be empty");
    }

    @Test
    void testFindByUserIdCaseType() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        mockSubscription.setCaseNumber(CASE_ID);
        mockSubscription.setCaseName(CASE_NAME);
        mockSubscription.setPartyNames(PARTY_NAMES);
        mockSubscription.setUrn(URN);

        CaseSubscription expected = new CaseSubscription();
        expected.setCaseNumber(CASE_ID);
        expected.setCaseName(CASE_NAME);
        expected.setUrn(URN);
        expected.setPartyNames(PARTY_NAMES);
        expected.setSubscriptionId(mockSubscription.getId());
        expected.setDateAdded(DATE_ADDED);
        expected.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        assertEquals(List.of(expected), userSubscriptionService.findByUserId(USER_ID).getCaseSubscriptions(),
                     "Should return populated case");
    }

    @Test
    void testFindByUserIdLength() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(mockSubscriptionList);
        when(subscriptionListTypeRepository
                 .findByUserId(any()))
            .thenReturn(Optional.of(mockSubscriptionListType));
        UserSubscription result = userSubscriptionService.findByUserId(USER_ID);
        assertEquals(6, result.getCaseSubscriptions().size(),
                     "Should add all CaseSubscriptions to UserSubscriptions");
        assertEquals(2, result.getLocationSubscriptions().size(), "Should add all court names");
    }

    @Test
    void testFindByUserId() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(mockSubscriptionList);
        when(subscriptionListTypeRepository
                 .findByUserId(any()))
                    .thenReturn(Optional.of(mockSubscriptionListType));
        UserSubscription result = userSubscriptionService.findByUserId(USER_ID);
        for (int i = 0; i < 6; i++) {
            assertEquals(CASE_ID + i, result.getCaseSubscriptions().get(i).getCaseNumber(),
                         "Should contain correct caseNumber");
        }
        assertEquals(COURT_NAME, result.getLocationSubscriptions().get(0).getLocationName(),
                     "Should match court name");
    }

    @Test
    void testFindByUserIdCreatedDates() {
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(mockSubscriptionList);
        when(subscriptionListTypeRepository
                 .findByUserId(any()))
            .thenReturn(Optional.of(mockSubscriptionListType));
        UserSubscription result = userSubscriptionService.findByUserId(USER_ID);
        for (int i = 0; i < 6; i++) {
            assertEquals(DATE_ADDED, result.getCaseSubscriptions().get(i).getDateAdded(),
                         "Should match dateAdded");
        }
        assertEquals(DATE_ADDED, result.getLocationSubscriptions().get(0).getDateAdded(), "Should match dateAdded");
    }

    @Test
    void testFindByUserIdAssignsIdForCourt() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));
        when(subscriptionListTypeRepository
                 .findByUserId(any()))
            .thenReturn(Optional.of(mockSubscriptionListType));

        assertEquals(mockSubscription.getId(),
                     userSubscriptionService.findByUserId(USER_ID)
                         .getLocationSubscriptions().get(0)
                         .getSubscriptionId(),
                     "Should match subscriptionId");
    }

    @Test
    void testFindByUserIdAssignsIdForCase() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        assertEquals(mockSubscription.getId(),
                     userSubscriptionService.findByUserId(USER_ID).getCaseSubscriptions().get(0).getSubscriptionId(),
                     "Should match subscriptionId");
    }

    @Test
    void testFindByUserIdListTypeSubscriptions() {
        mockSubscription.setSearchType(SearchType.LIST_TYPE);
        mockSubscription.setSearchValue(LIST_NAME);

        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        List<ListTypeSubscription> listTypeSubscriptions =
            userSubscriptionService.findByUserId(USER_ID).getListTypeSubscriptions();


        assertEquals(1, listTypeSubscriptions.size(), "Unexpected number of list type subscriptions returned");

        assertEquals(mockSubscription.getId(), listTypeSubscriptions.get(0).getSubscriptionId(),
                     "Returned list type subscription does not match ID");
        assertEquals(mockSubscription.getSearchValue(), listTypeSubscriptions.get(0).getListType(),
                     "Returned list type subscription does not match list type");
        assertEquals(mockSubscription.getChannel(), listTypeSubscriptions.get(0).getChannel(),
                     "Returned list type subscription does not match channel");
        assertEquals(mockSubscription.getCreatedDate(), listTypeSubscriptions.get(0).getDateAdded(),
                     "Returned list type subscription does not match created date");
    }

    @Test
    void testFindByUserIdNoListTypeSubscriptions() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        assertEquals(0, userSubscriptionService.findByUserId(USER_ID).getListTypeSubscriptions().size(),
                     "Unexpected number of list type subscriptions returned");
    }

    @Test
    void testDeleteAllByUserId() {
        UUID userIdWithSubs = UUID.randomUUID();
        doNothing().when(subscriptionRepository).deleteAllByUserId(userIdWithSubs);
        String result = userSubscriptionService.deleteAllByUserId(userIdWithSubs);
        assertTrue(
            result.contains(userIdWithSubs.toString()),
            "The service layer failed to delete the correct user id subscriptions");
    }
}
