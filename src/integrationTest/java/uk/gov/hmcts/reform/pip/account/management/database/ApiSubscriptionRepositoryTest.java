package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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

    private static final UUID USER_ID1 = UUID.randomUUID();
    private static final UUID USER_ID2 = UUID.randomUUID();

    @Autowired
    ApiSubscriptionRepository apiSubscriptionRepository;

    @BeforeAll
    void setup() {
        ApiSubscription apiSubscription1 = new ApiSubscription();
        apiSubscription1.setUserId(USER_ID1);
        apiSubscription1.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        apiSubscription1.setSensitivity(Sensitivity.PUBLIC);

        ApiSubscription apiSubscription2 = new ApiSubscription();
        apiSubscription2.setUserId(USER_ID1);
        apiSubscription2.setListType(ListType.FAMILY_DAILY_CAUSE_LIST);
        apiSubscription2.setSensitivity(Sensitivity.PRIVATE);

        ApiSubscription apiSubscription3 = new ApiSubscription();
        apiSubscription3.setUserId(USER_ID2);
        apiSubscription3.setListType(ListType.FAMILY_DAILY_CAUSE_LIST);
        apiSubscription3.setSensitivity(Sensitivity.CLASSIFIED);

        apiSubscriptionRepository.saveAll(List.of(apiSubscription1, apiSubscription2, apiSubscription3));
    }

    @AfterAll
    void shutdown() {
        apiSubscriptionRepository.deleteAll();
    }

    @Test
    void shouldFindApiSubscriptionsByUserId() {
        List<ApiSubscription> apiSubscriptions = apiSubscriptionRepository.findAllByUserId(USER_ID1);

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
    }

    @Test
    void shouldDeleteApiSubscriptionsByUserId() {
        assertThat(apiSubscriptionRepository.findAllByUserId(USER_ID2))
            .as("Third-party API subscription exists")
            .isNotEmpty();

        apiSubscriptionRepository.deleteAllByUserId(USER_ID2);

        assertThat(apiSubscriptionRepository.findAllByUserId(USER_ID2))
            .as("Third-party API subscription should be deleted")
            .isEmpty();
    }
}

