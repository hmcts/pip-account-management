package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import static uk.gov.hmcts.reform.pip.model.publication.ListType.CIVIL_DAILY_CAUSE_LIST;

@ActiveProfiles("non-async")
@ExtendWith({MockitoExtension.class})
class SubscriptionServiceTest {
    private static final String USER_ID = "Ralph21";
    private static final String SEARCH_VALUE = "193254";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final String ACTIONING_USER_ID = "1234-1234";
    private static final Integer LOCATION_ID = 1;

    public static final List<String> EXAMPLE_CSV_ALL = List.of(
        "a01d52c0-5c95-4f75-8994-a1c42cb45aaa,EMAIL,CASE_ID,2fe899ff-96ed-435a-bcad-1411bbe96d2a,1245,"
            + "2023-01-19 13:45:50.873778",
        "370963e2-9d2f-423e-b6a1-3f1f8905cdf0,EMAIL,CASE_ID,2fe899ff-96ed-435a-bcad-1411bbe96d2a,1234,"
            + "2023-01-19 13:47:23.484632",
        "052cda55-30fd-4a0d-939a-2c7b03ab3392,EMAIL,CASE_ID,2fe899ff-96ed-435a-bcad-1411bbe96d2a,1234,"
            + "2023-01-19 13:53:56.434343"
    );

    public static final List<String> EXAMPLE_CSV_LOCAL = List.of(
        "212c8b34-f6c3-424d-90e2-f874f528eebf,2,EMAIL,2fe899ff-96ed-435a-bcad-1411bbe96d2a,null,"
            + "2023-01-19 13:45:50.873778",
        "f4a0cb33-f211-4b46-8bdb-6320f6382a29,1234,API,2fe899ff-96ed-435a-bcad-1411bbe96d2a,null,"
            + "2023-01-19 13:47:23.484632",
        "34edfcde-4546-46b8-98e6-2717da3185e8,3,API,2fe899ff-96ed-435a-bcad-1411bbe96d2a,Oxford Combined Court Centre,"
            + "2023-01-19 13:53:56.434343"
    );

    private static final String COURT_NAME = "test court name";
    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();

    private static final String SUBSCRIPTION_CREATED_ERROR = "The returned subscription does "
        + "not match the expected subscription";

    private List<Subscription> mockSubscriptionList;
    private Subscription mockSubscription;
    private Subscription findableSubscription;
    private SubscriptionListType mockSubscriptionListType;

    @Mock
    SubscriptionLocationService subscriptionLocationService;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionListTypeRepository subscriptionListTypeRepository;

    @InjectMocks
    SubscriptionService subscriptionService;

    @BeforeEach
    void setup() {
        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, DATE_ADDED);
        mockSubscriptionList = createMockSubscriptionList(DATE_ADDED);
        findableSubscription = findableSubscription();
        mockSubscription.setChannel(Channel.EMAIL);
        mockSubscriptionListType = new SubscriptionListType(USER_ID,
            List.of(CIVIL_DAILY_CAUSE_LIST.name()), List.of("ENGLISH"));
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
    void testConfigureListTypesForLocationSubscription() {
        subscriptionService.configureListTypesForSubscription(mockSubscriptionListType, USER_ID);

        assertEquals(USER_ID, mockSubscription.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testConfigureEmptyListTypesForLocationSubscription() {
        mockSubscriptionListType.setListType(new ArrayList<>());
        subscriptionService.configureListTypesForSubscription(mockSubscriptionListType, USER_ID);

        assertEquals(USER_ID, mockSubscription.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
        );
    }

    @Test
    void testAddListTypesForLocationSubscription() {
        when(subscriptionListTypeRepository.save(mockSubscriptionListType))
            .thenReturn(mockSubscriptionListType);
        subscriptionService.addListTypesForSubscription(mockSubscriptionListType, USER_ID);

        assertEquals(USER_ID, mockSubscription.getUserId(),
                     SUBSCRIPTION_CREATED_ERROR
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
    void testMiServiceLocal() {
        when(subscriptionRepository.getLocalSubsDataForMi()).thenReturn(EXAMPLE_CSV_LOCAL);
        String testString = subscriptionService.getLocalSubscriptionsDataForMiReporting();
        String[] splitLineString = testString.split("\r\n|\r|\n");
        long countLine1 = splitLineString[0].chars().filter(character -> character == ',').count();

        assertThat(testString)
            .as("Json parsing has probably failed")
            .contains("Oxford")
            .hasLineCount(4);

        assertThat(splitLineString[0])
            .as("Header row does not match")
            .isEqualTo("id,search_value,channel,user_id,court_name,created_date");

        assertThat(splitLineString)
            .as("Wrong comma count compared to header row!")
            .allSatisfy(
                e -> assertThat(e.chars().filter(character -> character == ',').count()).isEqualTo(countLine1));

    }

    @Test
    void testMiServiceAll() {
        when(subscriptionRepository.getAllSubsDataForMi()).thenReturn(EXAMPLE_CSV_ALL);
        String testString = subscriptionService.getAllSubscriptionsDataForMiReporting();
        String[] splitLineString = testString.split("\r\n|\r|\n");
        long countLine1 = splitLineString[0].chars().filter(character -> character == ',').count();

        assertThat(testString)
            .as("Json parsing has probably failed")
            .contains("CASE_ID")
            .hasLineCount(4);

        assertThat(splitLineString[0])
            .as("Header row does not match")
            .isEqualTo("id,channel,search_type,user_id,court_name,created_date");

        assertThat(splitLineString)
            .as("Wrong comma count compared to header row!")
            .allSatisfy(
                e -> assertThat(e.chars().filter(character -> character == ',').count()).isEqualTo(countLine1));
    }
}

