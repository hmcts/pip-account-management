package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiSubscriptionRepositoryTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INVALID_USER_ID = UUID.randomUUID();

    @Autowired
    ApiSubscriptionRepository apiSubscriptionRepository;

    @AfterAll
    void shutdown() {
        apiSubscriptionRepository.deleteAll();
    }

    @Test
    void shouldFindAndDeleteApiSubscriptionsByUserId() {
        ApiSubscription apiSubscription1 = new ApiSubscription();
        apiSubscription1.setUserId(USER_ID);
        apiSubscription1.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        apiSubscription1.setSensitivity(Sensitivity.PUBLIC);

        ApiSubscription apiSubscription2 = new ApiSubscription();
        apiSubscription2.setUserId(USER_ID);
        apiSubscription2.setListType(ListType.FAMILY_DAILY_CAUSE_LIST);
        apiSubscription2.setSensitivity(Sensitivity.PRIVATE);

        apiSubscriptionRepository.saveAll(List.of(apiSubscription1, apiSubscription2));

        List<ApiSubscription> apiSubscriptions = apiSubscriptionRepository.findAllByUserId(USER_ID);

        assertThat(apiSubscriptions)
            .as("Third-party API subscription count does not match")
            .hasSize(2);

        assertThat(apiSubscriptions)
            .as("Third-party API subscription list types do not match")
            .extracting(ApiSubscription::getListType)
            .containsExactly(ListType.CIVIL_DAILY_CAUSE_LIST, ListType.FAMILY_DAILY_CAUSE_LIST);

        assertThat(apiSubscriptions)
            .as("Third-party API subscription sensitivities do not match")
            .extracting(ApiSubscription::getSensitivity)
            .containsExactly(Sensitivity.PUBLIC, Sensitivity.PRIVATE);

        apiSubscriptionRepository.deleteAllByUserId(USER_ID);

        assertThat(apiSubscriptionRepository.findAllByUserId(USER_ID))
            .as("Third-party API subscription should be deleted")
            .isEmpty();
    }

    @Test
    void shouldNotFindApiSubscriptionsByInvalidUser() {
        assertThat(apiSubscriptionRepository.findAllByUserId(INVALID_USER_ID))
            .as("Third-party API subscription should be empty")
            .isEmpty();
    }
}

