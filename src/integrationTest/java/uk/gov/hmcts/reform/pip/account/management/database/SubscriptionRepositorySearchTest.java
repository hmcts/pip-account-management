package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS, scripts = {"classpath:add-verified-users.sql"})
class SubscriptionRepositorySearchTest {
    private static final UUID USER_ID =  UUID.fromString("87f907d2-eb28-42cc-b6e1-ae2b03f7bba5");
    private static final String LOCATION_ID = "123";
    private static final String LOCATION_NAME = "Test location name";
    private static final String CASE_NUMBER = "Test case number";
    private static final String CASE_URN = "Test case URN";
    private static final String SUBSCRIPTION_MATCHED_MESSAGE = "Subscription does not match";
    private static final String SUBSCRIPTION_EMPTY_MESSAGE = "Subscription is not empty";
    private static final String LIST_LANGUAGE = "ENGLISH";

    private UUID subscriptionId1;
    private UUID subscriptionId2;
    private UUID subscriptionId3;
    private UUID subscriptionId4;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    SubscriptionListTypeRepository subscriptionListTypeRepository;

    @BeforeAll
    void setup() {
        Subscription subscription1 = new Subscription();
        subscription1.setUserId(USER_ID);
        subscription1.setSearchType(SearchType.LOCATION_ID);
        subscription1.setSearchValue(LOCATION_ID);
        subscription1.setChannel(Channel.EMAIL);
        subscription1.setLocationName(LOCATION_NAME);

        Subscription savedSubscription = subscriptionRepository.save(subscription1);
        subscriptionId1 = savedSubscription.getId();

        Subscription subscription2 = new Subscription();
        subscription2.setUserId(USER_ID);
        subscription2.setSearchType(SearchType.CASE_ID);
        subscription2.setSearchValue(CASE_NUMBER);
        subscription2.setChannel(Channel.EMAIL);
        subscription2.setCaseNumber(CASE_NUMBER);

        savedSubscription = subscriptionRepository.save(subscription2);
        subscriptionId2 = savedSubscription.getId();

        Subscription subscription3 = new Subscription();
        subscription3.setUserId(USER_ID);
        subscription3.setSearchType(SearchType.CASE_URN);
        subscription3.setSearchValue(CASE_URN);
        subscription3.setChannel(Channel.EMAIL);
        subscription3.setUrn(CASE_URN);

        savedSubscription = subscriptionRepository.save(subscription3);
        subscriptionId3 = savedSubscription.getId();

        Subscription subscription4 = new Subscription();
        subscription4.setUserId(USER_ID);
        subscription4.setSearchType(SearchType.LIST_TYPE);
        subscription4.setSearchValue(ListType.CIVIL_DAILY_CAUSE_LIST.name());
        subscription4.setChannel(Channel.EMAIL);
        subscription4.setLocationName(LOCATION_NAME);
        savedSubscription = subscriptionRepository.save(subscription4);
        subscriptionId4 = savedSubscription.getId();

        SubscriptionListType subscriptionListType = new SubscriptionListType();
        subscriptionListType.setListType(List.of(ListType.CIVIL_DAILY_CAUSE_LIST.name()));
        subscriptionListType.setUserId(USER_ID);
        subscriptionListType.setListLanguage(List.of(LIST_LANGUAGE));
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    @AfterAll
    void shutdown() {
        subscriptionRepository.deleteAll();
    }

    @Test
    void shouldFindSubscriptionsByCaseIdSearchValue() {
        assertThat(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_ID.name(), CASE_NUMBER))
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .hasSize(1).extracting(Subscription::getId)
            .containsExactly(subscriptionId2);
    }

    @Test
    void shouldFindNotSubscriptionsByCaseIdUsingCaseUrnSearchValue() {
        assertThat(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_ID.name(), CASE_URN))
            .as(SUBSCRIPTION_EMPTY_MESSAGE)
            .isEmpty();
    }

    @Test
    void shouldFindSubscriptionsByCaseUrnSearchValue() {
        assertThat(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_URN.name(), CASE_URN))
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .hasSize(1).extracting(Subscription::getId)
            .containsExactly(subscriptionId3);
    }

    @Test
    void shouldNotFindSubscriptionsByCaseUrnUsingCaseNumberSearchValue() {
        assertThat(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.CASE_URN.name(), CASE_NUMBER))
            .as(SUBSCRIPTION_EMPTY_MESSAGE)
            .isEmpty();
    }

    @Test
    void shouldFindSubscriptionsByListTypeSearchValue() {
        assertThat(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.LIST_TYPE.name(),
                                                                         ListType.CIVIL_DAILY_CAUSE_LIST.name()))
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .hasSize(1)
            .extracting(Subscription::getId)
            .containsExactlyInAnyOrder(subscriptionId4);
    }

    @Test
    void shouldNotFindSubscriptionsByListTypeSearchValueIfListTypeNotMatched() {
        assertThat(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.LIST_TYPE.name(),
                                                                         ListType.FAMILY_DAILY_CAUSE_LIST.name()))
            .as(SUBSCRIPTION_EMPTY_MESSAGE)
            .isEmpty();
    }

    @Test
    void shouldNotFindSubscriptionsByLocationIdUsingStandardSearch() {
        assertThat(subscriptionRepository.findSubscriptionsBySearchValue(SearchType.LOCATION_ID.name(), LOCATION_ID))
            .as(SUBSCRIPTION_EMPTY_MESSAGE)
            .isEmpty();
    }

    @Test
    void shouldFindSubscriptionsByLocationIdSearchValue() {
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsByLocationSearchValue(
            LOCATION_ID, ListType.CIVIL_DAILY_CAUSE_LIST.name(), LIST_LANGUAGE
        );
        assertThat(subscriptions)
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .hasSize(1)
            .extracting(Subscription::getId)
            .containsExactly(subscriptionId1);
    }

    @Test
    void shouldNotFindSubscriptionsByLocationIdSearchValueIfListTypeUnmatched() {
        List<Subscription> subscriptions = subscriptionRepository.findSubscriptionsByLocationSearchValue(
            LOCATION_ID, ListType.CROWN_DAILY_LIST.name(), LIST_LANGUAGE
        );
        assertThat(subscriptions)
            .as(SUBSCRIPTION_EMPTY_MESSAGE)
            .isEmpty();
    }
}
