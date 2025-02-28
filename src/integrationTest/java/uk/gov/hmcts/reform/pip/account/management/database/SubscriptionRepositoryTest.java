package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubscriptionRepositoryTest {
    private static final UUID USER_ID1 = UUID.randomUUID();
    private static final UUID USER_ID2 = UUID.randomUUID();
    private static final String LOCATION_ID1 = "123";
    private static final String LOCATION_ID2 = "124";
    private static final String LOCATION_ID3 = "125";
    private static final String INVALID_LOCATION_ID = "111";
    private static final String LOCATION_NAME1 = "Test location name";
    private static final String LOCATION_NAME2 = "Test location name 2";
    private static final String LOCATION_NAME3 = "Test location name 3";
    private static final String CASE_NUMBER = "Test case number";
    private static final LocalDateTime SUBSCRIPTION_CREATED_DATE = LocalDateTime.of(2025, 2, 5, 2, 2, 2);

    private static final String SUBSCRIPTION_MATCHED_MESSAGE = "Subscription does not match";
    private static final String SUBSCRIPTION_EMPTY_MESSAGE = "Subscription is not empty";

    private UUID subscriptionId2;
    private UUID subscriptionId4;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @BeforeAll
    void setup() {
        Subscription subscription1 = new Subscription();
        subscription1.setUserId(USER_ID1);
        subscription1.setSearchType(SearchType.LOCATION_ID);
        subscription1.setSearchValue(LOCATION_ID1);
        subscription1.setChannel(Channel.EMAIL);
        subscription1.setLocationName(LOCATION_NAME1);
        subscriptionRepository.save(subscription1);

        Subscription subscription2 = new Subscription();
        subscription2.setUserId(USER_ID1);
        subscription2.setSearchType(SearchType.LOCATION_ID);
        subscription2.setSearchValue(LOCATION_ID2);
        subscription2.setChannel(Channel.EMAIL);
        subscription2.setLocationName(LOCATION_NAME2);
        subscription2.setCreatedDate(SUBSCRIPTION_CREATED_DATE);


        Subscription savedSubscription = subscriptionRepository.save(subscription2);
        subscriptionId2 = savedSubscription.getId();

        Subscription subscription3 = new Subscription();
        subscription3.setUserId(USER_ID2);
        subscription3.setSearchType(SearchType.LOCATION_ID);
        subscription3.setSearchValue(LOCATION_ID3);
        subscription3.setChannel(Channel.EMAIL);
        subscription3.setLocationName(LOCATION_NAME3);
        subscriptionRepository.save(subscription3);

        Subscription subscription4 = new Subscription();
        subscription4.setUserId(USER_ID1);
        subscription4.setSearchType(SearchType.CASE_ID);
        subscription4.setSearchValue(CASE_NUMBER);
        subscription4.setChannel(Channel.EMAIL);
        subscription4.setCaseNumber(CASE_NUMBER);
        subscription4.setCreatedDate(SUBSCRIPTION_CREATED_DATE);

        savedSubscription = subscriptionRepository.save(subscription4);
        subscriptionId4 = savedSubscription.getId();
    }

    @AfterAll
    void shutdown() {
        subscriptionRepository.deleteAll();
    }

    @Test
    void shouldGetAllSubscriptionDataForMiV2() {
        List<AllSubscriptionMiData> subscriptionMiDataList = subscriptionRepository.getAllSubsDataForMiV2();

        assertThat(subscriptionMiDataList)
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .hasSize(4)
            .anyMatch(subscription -> subscriptionId4.equals(subscription.getId())
                && USER_ID1.equals(subscription.getUserId())
                && subscription.getChannel().equals(Channel.EMAIL)
                && subscription.getSearchType().equals(SearchType.CASE_ID)
                && SUBSCRIPTION_CREATED_DATE.equals(subscription.getCreatedDate()))
            .anyMatch(subscription -> subscriptionId2.equals(subscription.getId())
                && subscription.getChannel().equals(Channel.EMAIL)
                && subscription.getSearchType().equals(SearchType.LOCATION_ID)
                && USER_ID1.equals(subscription.getUserId())
                && LOCATION_NAME2.equals(subscription.getLocationName())
                && SUBSCRIPTION_CREATED_DATE.equals(subscription.getCreatedDate()));
    }

    @Test
    void shouldGetLocationSubscriptionDataForMiV2() {
        List<LocationSubscriptionMiData> subscriptionMiDataList = subscriptionRepository.getLocationSubsDataForMiV2();

        assertThat(subscriptionMiDataList)
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .hasSize(3)
            .anyMatch(subscription -> subscriptionId2.equals(subscription.getId())
                && subscription.getChannel().equals(Channel.EMAIL)
                && LOCATION_ID2.equals(subscription.getSearchValue())
                && USER_ID1.equals(subscription.getUserId())
                && LOCATION_NAME2.equals(subscription.getLocationName())
                && SUBSCRIPTION_CREATED_DATE.equals(subscription.getCreatedDate()))
            .noneMatch(subscription -> subscriptionId4.equals(subscription.getId()));
    }

    @Test
    void shouldFindSubscriptionsByLocationId() {
        assertThat(subscriptionRepository.findSubscriptionsByLocationId(LOCATION_ID2))
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .hasSize(1)
            .extracting(Subscription::getId)
            .containsExactly(subscriptionId2);
    }

    @Test
    void shouldNotFindSubscriptionsByLocationIdUsingInvalidValue() {
        assertThat(subscriptionRepository.findSubscriptionsByLocationId(INVALID_LOCATION_ID))
            .as(SUBSCRIPTION_EMPTY_MESSAGE)
            .isEmpty();
    }
}
