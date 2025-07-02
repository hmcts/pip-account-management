package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.createMockSubscription;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.createMockSubscriptionList;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.findableSubscription;

@ActiveProfiles("non-async")
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID UNKNOWN_USER_ID = UUID.randomUUID();
    private static final String SEARCH_VALUE = "193254";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final String ACTIONING_USER_ID = "1234-1234";

    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();
    private static final String SUBSCRIPTION_CREATED_ERROR = "The returned subscription does "
        + "not match the expected subscription";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;

    @Mock
    SubscriptionListTypeService subscriptionListTypeService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    SubscriptionService subscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, DATE_ADDED);
        mockSubscriptionList = createMockSubscriptionList(DATE_ADDED);
        findableSubscription = findableSubscription();
        mockSubscription.setChannel(Channel.EMAIL);

        Mockito.lenient().when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(new PiUser()));
        Mockito.lenient().when(userRepository.findByUserId(UNKNOWN_USER_ID)).thenReturn(Optional.empty());
    }

    @Test
    void testGetSubscriptionReturnsExpected() {
        when(subscriptionRepository.findAll()).thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList, subscriptionService.findAll(), "The returned subscription list "
            + "does not match the expected list");
    }

    @Test
    void testCreateSubscription() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateSubscriptionWhenUnknownUser() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        mockSubscription.setUserId(UNKNOWN_USER_ID);

        assertThrows(UserNotFoundException.class, () ->
            subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID),
                     "UserNotFoundException not thrown when user is not present");
    }

    @Test
    void testLastUpdatedDateIsSet() {
        ArgumentCaptor<Subscription> argumentCaptor = ArgumentCaptor.forClass(Subscription.class);

        mockSubscription.setSearchType(SearchType.CASE_ID);
        when(subscriptionRepository.save(argumentCaptor.capture())).thenReturn(mockSubscription);

        subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID);

        Subscription subscription = argumentCaptor.getValue();

        assertEquals(subscription.getCreatedDate(), subscription.getLastUpdatedDate(),
                     "Last updated date should be equal to created date"
        );
    }

    @Test
    void testCreateSubscriptionWithCourtName() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateSubscriptionWithCourtNameWithoutListType() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateSubscriptionWithCourtNameWithMultipleListType() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        assertEquals(subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID), mockSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testCreateDuplicateSubscription() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setSearchValue(SEARCH_VALUE);
        when(subscriptionRepository.save(mockSubscription)).thenReturn(mockSubscription);
        when(subscriptionRepository.findByUserId(USER_ID)).thenReturn(List.of(mockSubscription));

        Subscription returnedSubscription =
            subscriptionService.createSubscription(mockSubscription, ACTIONING_USER_ID);

        verify(subscriptionRepository, times(1)).delete(mockSubscription);
        assertEquals(returnedSubscription, mockSubscription,
                     "The Returned subscription does match the expected subscription"
        );
    }

    @Test
    void testDeleteSubscriptionWhereUserHasNoLocationSubscriptionAfterDeletion() {
        UUID testUuid = UUID.randomUUID();

        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        when(subscriptionRepository.findLocationSubscriptionsByUserId(any())).thenReturn(Collections.emptyList());

        subscriptionService.deleteById(testUuid, ACTIONING_USER_ID);
        verify(subscriptionRepository).deleteById(testUuid);
        verify(subscriptionListTypeService).deleteListTypesForSubscription(any());
    }

    @Test
    void testDeleteSubscriptionWhereUserStillHasLocationSubscriptionAfterDeletion() {
        UUID testUuid = UUID.randomUUID();

        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        when(subscriptionRepository.findLocationSubscriptionsByUserId(any())).thenReturn(mockSubscriptionList);

        subscriptionService.deleteById(testUuid, ACTIONING_USER_ID);
        verify(subscriptionRepository).deleteById(testUuid);
        verify(subscriptionListTypeService, never()).deleteListTypesForSubscription(any());
    }

    @Test
    void testDeleteException() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.deleteById(
                         testUuid, ACTIONING_USER_ID),
                     "SubscriptionNotFoundException not thrown when trying to delete a subscription"
                         + " that does not exist"
        );
    }

    @Test
    void testBulkDeleteSubscriptionsWhereUserHasNoLocationSubscriptionAfterDeletion() {
        List<UUID> testIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Subscription subscription1 = createMockSubscription(USER_ID, "1", EMAIL, DATE_ADDED);
        Subscription subscription2 = createMockSubscription(USER_ID, "2", EMAIL, DATE_ADDED);

        when(subscriptionRepository.findByIdIn(testIds)).thenReturn(List.of(subscription1, subscription2));
        when(subscriptionRepository.findLocationSubscriptionsByUserId(USER_ID)).thenReturn(Collections.emptyList());

        subscriptionService.bulkDeleteSubscriptions(testIds);
        verify(subscriptionRepository).deleteByIdIn(testIds);
        verify(subscriptionListTypeService).deleteListTypesForSubscription(USER_ID);
    }

    @Test
    void testBulkDeleteSubscriptionsWhereUserStillHasLocationSubscriptionAfterDeletion() {
        List<UUID> testIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Subscription subscription1 = createMockSubscription(USER_ID, "1", EMAIL, DATE_ADDED);
        Subscription subscription2 = createMockSubscription(USER_ID, "2", EMAIL, DATE_ADDED);

        when(subscriptionRepository.findByIdIn(testIds)).thenReturn(List.of(subscription1, subscription2));
        when(subscriptionRepository.findLocationSubscriptionsByUserId(USER_ID)).thenReturn(mockSubscriptionList);

        subscriptionService.bulkDeleteSubscriptions(testIds);
        verify(subscriptionRepository).deleteByIdIn(testIds);
        verify(subscriptionListTypeService, never()).deleteListTypesForSubscription(USER_ID);
    }

    @Test
    void testFindException() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.empty());
        assertThrows(SubscriptionNotFoundException.class, () -> subscriptionService.findById(testUuid),
                     "SubscriptionNotFoundException not thrown "
                         + "when trying to find a subscription that does not exist"
        );
    }

    @Test
    void testFindSubscription() {
        UUID testUuid = UUID.randomUUID();
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        assertEquals(subscriptionService.findById(testUuid), findableSubscription,
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testMiServiceLocation() {
        LocationSubscriptionMiData locationSubscriptionMiData = new LocationSubscriptionMiData();
        locationSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionRepository.getLocationSubsDataForMi())
            .thenReturn(List.of(locationSubscriptionMiData));

        List<LocationSubscriptionMiData> locationSubscriptionsMiDataList = subscriptionService
            .getLocationSubscriptionsDataForMiReporting();

        assertThat(locationSubscriptionsMiDataList).contains(locationSubscriptionMiData);
    }

    @Test
    void testMiServiceAll() {
        AllSubscriptionMiData allSubscriptionMiData = new AllSubscriptionMiData();
        allSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionRepository.getAllSubsDataForMi())
            .thenReturn(List.of(allSubscriptionMiData));

        List<AllSubscriptionMiData> allSubscriptionsMiDataList = subscriptionService
            .getAllSubscriptionsDataForMiReporting();

        assertThat(allSubscriptionsMiDataList).contains(allSubscriptionMiData);
    }
}

