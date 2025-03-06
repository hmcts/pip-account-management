package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.model.publication.ListType;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubscriptionListTypeRepositoryTest {

    private static final String USER_ID = "1";
    private static final String LIST_LANGUAGE = "ENGLISH";
    private static final List<String> LIST_TYPE = List.of(
        ListType.CIVIL_DAILY_CAUSE_LIST.name(),
        ListType.FAMILY_DAILY_CAUSE_LIST.name(),
        ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST.name()
    );

    private SubscriptionListType subscriptionListType;

    private static final String SUBSCRIPTION_MATCHED_MESSAGE = "Subscription does not match";

    @Autowired
    SubscriptionListTypeRepository subscriptionListTypeRepository;

    @BeforeAll
    void setup() {
        subscriptionListType = new SubscriptionListType();
        subscriptionListType.setListType(LIST_TYPE);
        subscriptionListType.setUserId(USER_ID);
        subscriptionListType.setListLanguage(List.of(LIST_LANGUAGE));
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    @AfterAll
    void shutdown() {
        subscriptionListTypeRepository.deleteAll();
    }

    @Test
    void shouldFindSubscriptionListTypeByUserId() {
        assertThat(subscriptionListTypeRepository.findByUserId(USER_ID))
            .as(SUBSCRIPTION_MATCHED_MESSAGE)
            .contains(subscriptionListType);
    }
}
