package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.createMockSubscription;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.createMockSubscriptionList;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.findableSubscription;

@ActiveProfiles("non-async")
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    private static final String USER_ID = "Ralph21";
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
    SubscriptionLocationService subscriptionLocationService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @InjectMocks
    SubscriptionService subscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, DATE_ADDED);
        mockSubscriptionList = createMockSubscriptionList(DATE_ADDED);
        findableSubscription = findableSubscription();
        mockSubscription.setChannel(Channel.EMAIL);
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
    void testDeleteSubscription() {
        UUID testUuid = UUID.randomUUID();
        ArgumentCaptor<UUID> captor = ArgumentCaptor.forClass(UUID.class);
        doNothing().when(subscriptionRepository).deleteById(captor.capture());
        when(subscriptionRepository.findById(testUuid)).thenReturn(Optional.of(findableSubscription));
        doNothing().when(subscriptionLocationService).deleteSubscriptionListTypeByUser(any());
        subscriptionService.deleteById(testUuid, ACTIONING_USER_ID);
        assertEquals(testUuid, captor.getValue(), "The service layer tried to delete the wrong subscription");
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
    void testMiServiceLocationV2() {
        LocationSubscriptionMiData locationSubscriptionMiData = new LocationSubscriptionMiData();
        locationSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionRepository.getLocationSubsDataForMiV2())
            .thenReturn(List.of(locationSubscriptionMiData));

        List<LocationSubscriptionMiData> locationSubscriptionsMiDataList = subscriptionService
            .getLocationSubscriptionsDataForMiReportingV2();

        assertThat(locationSubscriptionsMiDataList).contains(locationSubscriptionMiData);
    }

    @Test
    void testMiServiceAllV2() {
        AllSubscriptionMiData allSubscriptionMiData = new AllSubscriptionMiData();
        allSubscriptionMiData.setId(UUID.randomUUID());

        when(subscriptionRepository.getAllSubsDataForMiV2())
            .thenReturn(List.of(allSubscriptionMiData));

        List<AllSubscriptionMiData> allSubscriptionsMiDataList = subscriptionService
            .getAllSubscriptionsDataForMiReportingV2();

        assertThat(allSubscriptionsMiDataList).contains(allSubscriptionMiData);
    }
}

