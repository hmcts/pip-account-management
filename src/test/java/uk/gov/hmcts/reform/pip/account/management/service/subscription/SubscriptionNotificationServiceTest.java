package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionsSummaryDetails;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartySubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.Language;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.helpers.SubscriptionUtils.createMockSubscription;

@ExtendWith({MockitoExtension.class})
@MockitoSettings(strictness = Strictness.LENIENT)
class SubscriptionNotificationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String SEARCH_VALUE = "193254";
    private static final String CASE_ID = "123";
    private static final Channel EMAIL = Channel.EMAIL;
    private static final String COURT_MATCH = "1";
    private static final String CASE_MATCH = "case match";
    private static final UUID ACCEPTED_USER_ID = UUID.randomUUID();
    private static final UUID FORBIDDEN_USER_ID = UUID.randomUUID();
    private static final String SUBSCRIBER_NOTIFICATION_LOG = "Summary being sent to publication services for id";
    private static final String LOG_MESSAGE_MATCH = "Log messages should match.";
    private static final String CASE_NUMBER_KEY = "caseNumber";
    private static final String CASE_URN_KEY = "caseUrn";
    private static final UUID TEST_UUID = UUID.randomUUID();
    private static final String TEST_USER_EMAIL = "a@b.com";
    private static final String TEST = "test";
    private static final String CASE_ID_SEARCH = SearchType.CASE_ID.name();
    private static final String LIST_TYPE_SEARCH = SearchType.LIST_TYPE.name();
    private static final String MAGISTRATES_PUBLIC_LIST = ListType.MAGISTRATES_PUBLIC_LIST.name();
    private static final String LIST_LANGUAGE = "ENGLISH";

    private Subscription mockSubscription;
    private final SubscriptionsSummary mockSubscriptionsSummary = new SubscriptionsSummary();
    private final SubscriptionsSummaryDetails mockSubscriptionsSummaryDetails = new SubscriptionsSummaryDetails();

    private final Artefact classifiedArtefactMatches = new Artefact();
    private final Artefact publicArtefactMatches = new Artefact();
    private final Subscription returnedSubscription = new Subscription();
    private final Subscription restrictedSubscription = new Subscription();
    private final List<Object> cases = new ArrayList<>();
    private final Map<String, List<Object>> searchTerms = new ConcurrentHashMap<>();
    private final Map<String, List<Subscription>> returnedMappedEmails = new ConcurrentHashMap<>();

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionChannelService subscriptionChannelService;

    @Mock
    AccountService accountService;

    @Mock
    PublicationService publicationService;

    @Mock
    ThirdPartySubscriptionNotificationService thirdPartySubscriptionNotificationService;

    @InjectMocks
    SubscriptionNotificationService subscriptionNotificationService;

    @BeforeEach
    void setup() {
        Map<String, String> map = new ConcurrentHashMap<>();
        Map<String, String> map2 = new ConcurrentHashMap<>();
        map.put(CASE_NUMBER_KEY, CASE_MATCH);
        map.put(CASE_URN_KEY, TEST);
        map2.put(CASE_NUMBER_KEY, TEST);
        map2.put(CASE_URN_KEY, CASE_MATCH);

        cases.add(map);
        cases.add(map2);
        searchTerms.put("cases", cases);

        classifiedArtefactMatches.setArtefactId(TEST_UUID);
        classifiedArtefactMatches.setSensitivity(Sensitivity.CLASSIFIED);
        classifiedArtefactMatches.setSearch(searchTerms);
        classifiedArtefactMatches.setLocationId(COURT_MATCH);
        classifiedArtefactMatches.setListType(ListType.SJP_PRESS_LIST);
        classifiedArtefactMatches.setLanguage(Language.ENGLISH);

        publicArtefactMatches.setArtefactId(TEST_UUID);
        publicArtefactMatches.setSensitivity(Sensitivity.PUBLIC);
        publicArtefactMatches.setLocationId(COURT_MATCH);
        publicArtefactMatches.setSearch(searchTerms);
        publicArtefactMatches.setListType(ListType.MAGISTRATES_PUBLIC_LIST);
        publicArtefactMatches.setLanguage(Language.ENGLISH);

        returnedSubscription.setUserId(ACCEPTED_USER_ID);
        restrictedSubscription.setUserId(FORBIDDEN_USER_ID);

        mockSubscription = createMockSubscription(USER_ID, SEARCH_VALUE, EMAIL, LocalDateTime.now());

        mockSubscriptionsSummary.setEmail(TEST_USER_EMAIL);
        mockSubscription.setChannel(Channel.EMAIL);

    }

    @Test
    void testCollectSubscribersCourtSubscriptionNotClassified() throws IOException {
        returnedSubscription.setChannel(Channel.EMAIL);
        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(returnedSubscription));
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(returnedSubscription));
        when(subscriptionChannelService.buildEmailSubscriptions(any())).thenReturn(returnedMappedEmails);
        doNothing().when(publicationService).postSubscriptionSummaries(any(), any());
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersCourtSubscriptionWithListTypeNotClassified() throws IOException {
        returnedSubscription.setChannel(Channel.EMAIL);
        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(returnedSubscription));
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(returnedSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(any())).thenReturn(returnedMappedEmails);
        doNothing().when(publicationService).postSubscriptionSummaries(any(), any());
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersCaseUrn() {
        mockSubscription.setSearchType(SearchType.CASE_URN);
        mockSubscription.setSearchValue(CASE_URN_KEY);

        mockSubscriptionsSummaryDetails.addToCaseUrn(CASE_URN_KEY);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(List.of(mockSubscription)))
            .thenReturn(returnedMappedEmails);

        doNothing().when(publicationService).postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                       returnedMappedEmails);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectSubscribersCaseUrnNull() {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put(CASE_URN_KEY, "Case Urn");
        cases.add(map);
        searchTerms.put("cases", cases);
        publicArtefactMatches.setSearch(searchTerms);
        mockSubscription.setSearchType(SearchType.CASE_URN);
        mockSubscription.setSearchValue(CASE_URN_KEY);

        mockSubscriptionsSummaryDetails.addToCaseUrn(CASE_URN_KEY);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(List.of(mockSubscription)))
            .thenReturn(returnedMappedEmails);

        doNothing().when(publicationService).postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                       returnedMappedEmails);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectSubscribersCaseNumberNull() {
        Map<String, String> map = new ConcurrentHashMap<>();
        map.put(CASE_NUMBER_KEY, "Case Number");
        cases.add(map);
        searchTerms.put("cases", cases);
        publicArtefactMatches.setSearch(searchTerms);
        mockSubscription.setSearchType(SearchType.CASE_ID);
        mockSubscription.setSearchValue(CASE_NUMBER_KEY);

        mockSubscriptionsSummaryDetails.addToCaseNumber(CASE_NUMBER_KEY);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(List.of(mockSubscription)))
            .thenReturn(returnedMappedEmails);

        doNothing().when(publicationService).postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                       returnedMappedEmails);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectSubscribersCaseId() {
        mockSubscription.setSearchType(SearchType.CASE_ID);
        mockSubscription.setSearchValue(CASE_ID);

        mockSubscriptionsSummaryDetails.addToCaseNumber(CASE_ID);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(List.of(mockSubscription)))
            .thenReturn(returnedMappedEmails);

        doNothing().when(publicationService).postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                       returnedMappedEmails);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectSubscribersLocationId() {
        mockSubscription.setSearchType(SearchType.LOCATION_ID);
        mockSubscription.setSearchValue(COURT_MATCH);

        mockSubscriptionsSummaryDetails.addToLocationId(COURT_MATCH);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(mockSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(List.of(mockSubscription)))
            .thenReturn(returnedMappedEmails);

        doNothing().when(publicationService).postSubscriptionSummaries(publicArtefactMatches.getArtefactId(),
                                                                       returnedMappedEmails);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);

            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectListTypeSubscription() throws IOException {
        mockSubscription.setSearchType(SearchType.LIST_TYPE);
        mockSubscription.setSearchValue(MAGISTRATES_PUBLIC_LIST);
        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(mockSubscription));
        when(subscriptionRepository.findSubscriptionsBySearchValue(LIST_TYPE_SEARCH, MAGISTRATES_PUBLIC_LIST))
            .thenReturn(List.of(mockSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(List.of(mockSubscription)))
            .thenReturn(returnedMappedEmails);
        doNothing().when(publicationService).postSubscriptionSummaries(any(), any());

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG),
                       LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectApiSubscribers() throws IOException {
        mockSubscription.setChannel(Channel.API_COURTEL);
        Map<String, List<Subscription>> returnedMap = new ConcurrentHashMap<>();
        returnedMap.put(TEST, List.of(mockSubscription));
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(mockSubscription));
        when(subscriptionChannelService.buildApiSubscriptions(List.of(mockSubscription))).thenReturn(returnedMap);
        doNothing().when(publicationService).sendThirdPartyList(any(ThirdPartySubscription.class));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(publicArtefactMatches);
            verify(thirdPartySubscriptionNotificationService).handleThirdPartySubscription(publicArtefactMatches);
            assertTrue(logCaptor.getErrorLogs().isEmpty(), LOG_MESSAGE_MATCH);
        } catch (Exception ex) {
            throw new IOException(ex.getMessage());
        }
    }

    @Test
    void testCollectSubscribersRestrictsClassified() {
        returnedSubscription.setChannel(Channel.EMAIL);
        returnedSubscription.setUserId(ACCEPTED_USER_ID);
        returnedSubscription.setSearchType(SearchType.CASE_ID);
        returnedSubscription.setSearchValue(CASE_ID);
        restrictedSubscription.setChannel(Channel.EMAIL);
        restrictedSubscription.setUserId(FORBIDDEN_USER_ID);
        returnedSubscription.setSearchType(SearchType.CASE_ID);
        returnedSubscription.setSearchValue(CASE_ID);

        mockSubscriptionsSummaryDetails.addToCaseNumber(CASE_ID);
        mockSubscriptionsSummary.setSubscriptions(mockSubscriptionsSummaryDetails);

        when(subscriptionRepository.findSubscriptionsBySearchValue(CASE_ID_SEARCH, CASE_MATCH))
            .thenReturn(List.of(returnedSubscription, restrictedSubscription));

        when(accountService.isUserAuthorisedForPublication(
            ACCEPTED_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.CLASSIFIED)).thenReturn(true);
        when(accountService.isUserAuthorisedForPublication(
            FORBIDDEN_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.CLASSIFIED))
            .thenReturn(false);

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(returnedSubscription));
        when(subscriptionChannelService.buildEmailSubscriptions(List.of(returnedSubscription)))
            .thenReturn(returnedMappedEmails);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectSubscribers(classifiedArtefactMatches);
            assertTrue(logCaptor.getInfoLogs().get(0).contains(SUBSCRIBER_NOTIFICATION_LOG), LOG_MESSAGE_MATCH);
            assertTrue(logCaptor.getErrorLogs().isEmpty(), LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testNoValidSubscriptionsDoesNotCallPostSubscriptionSummaries() {
        returnedSubscription.setChannel(Channel.EMAIL);
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(returnedSubscription));
        when(subscriptionChannelService.buildEmailSubscriptions(any())).thenReturn(new ConcurrentHashMap<>());

        subscriptionNotificationService.collectSubscribers(publicArtefactMatches);
        verify(publicationService, never()).postSubscriptionSummaries(any(), any());
    }

    @Test
    void testMultipleSubscriptionsIsPassedToPostSubscriptionSummaries() {
        returnedSubscription.setChannel(Channel.EMAIL);
        when(subscriptionRepository.findSubscriptionsByLocationSearchValue(COURT_MATCH,
                                                                           MAGISTRATES_PUBLIC_LIST, LIST_LANGUAGE))
            .thenReturn(List.of(returnedSubscription, returnedSubscription));

        returnedMappedEmails.put(TEST_USER_EMAIL, List.of(returnedSubscription));
        returnedMappedEmails.put("SecondUserEmail", List.of(returnedSubscription));

        when(subscriptionChannelService.buildEmailSubscriptions(any())).thenReturn(returnedMappedEmails);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, List<Subscription>>> argument = ArgumentCaptor.forClass(Map.class);
        doNothing().when(publicationService).postSubscriptionSummaries(
            eq(publicArtefactMatches.getArtefactId()), argument.capture());

        subscriptionNotificationService.collectSubscribers(publicArtefactMatches);

        Map<String, List<Subscription>> capturedMap = argument.getValue();
        assertEquals(2, capturedMap.size(), "The size of the captured map is incorrect");
    }

    @Test
    void testCollectThirdPartyForDeletion() {
        mockSubscription.setChannel(Channel.API_COURTEL);
        Map<String, List<Subscription>> returnedMap = Collections.singletonMap(TEST, List.of(mockSubscription));
        when(subscriptionRepository.findSubscriptionsBySearchValue(LIST_TYPE_SEARCH,
                                                                   publicArtefactMatches.getListType().name()))
            .thenReturn(List.of(mockSubscription));
        when(subscriptionChannelService.buildApiSubscriptions(List.of(mockSubscription))).thenReturn(returnedMap);
        doNothing().when(publicationService).sendEmptyArtefact(any(ThirdPartySubscriptionArtefact.class));
        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionNotificationService.class)) {
            subscriptionNotificationService.collectThirdPartyForDeletion(publicArtefactMatches);
            verify(thirdPartySubscriptionNotificationService)
                .handleThirdPartySubscriptionForDeletedPublication(publicArtefactMatches);
            assertTrue(logCaptor.getErrorLogs().isEmpty(), LOG_MESSAGE_MATCH);
        }
    }

    @Test
    void testCollectThirdPartyForDeletionClassifiedExcluded() {
        mockSubscription.setChannel(Channel.API_COURTEL);
        Map<String, List<Subscription>> returnedMap = new ConcurrentHashMap<>();
        returnedMap.put(TEST, List.of(mockSubscription));
        when(subscriptionRepository.findSubscriptionsBySearchValue(LIST_TYPE_SEARCH,
                                                                   classifiedArtefactMatches.getListType().name()))
            .thenReturn(List.of(mockSubscription));
        when(accountService.isUserAuthorisedForPublication(USER_ID,
                                                           classifiedArtefactMatches.getListType(),
                                                           classifiedArtefactMatches.getSensitivity()))
            .thenReturn(false);
        subscriptionNotificationService.collectThirdPartyForDeletion(classifiedArtefactMatches);
        ThirdPartySubscriptionArtefact subscriptionArtefact = new ThirdPartySubscriptionArtefact(
            TEST, classifiedArtefactMatches);
        verify(publicationService, never()).sendEmptyArtefact(subscriptionArtefact);
    }
}
