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
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
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
    private static final UUID INVALID_USER_ID = UUID.randomUUID();
    private static final String USER_NAME = "Test name";

    private UUID userId;

    @Autowired
    ApiSubscriptionRepository apiSubscriptionRepository;

    @Autowired
    ApiUserRepository apiUserRepository;

    @BeforeAll
    void setup() {
        ApiUser apiUser = new ApiUser();
        apiUser.setName(USER_NAME);
        ApiUser createdApiUser = apiUserRepository.save(apiUser);
        userId = createdApiUser.getUserId();

        ApiSubscription apiSubscription1 = new ApiSubscription();
        apiSubscription1.setUserId(userId);
        apiSubscription1.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        apiSubscription1.setSensitivity(Sensitivity.PUBLIC);

        ApiSubscription apiSubscription2 = new ApiSubscription();
        apiSubscription2.setUserId(userId);
        apiSubscription2.setListType(ListType.FAMILY_DAILY_CAUSE_LIST);
        apiSubscription2.setSensitivity(Sensitivity.PRIVATE);

        apiSubscriptionRepository.saveAll(List.of(apiSubscription1, apiSubscription2));

        ApiUser apiUser2 = new ApiUser();
        apiUser2.setName(USER_NAME);
        ApiUser createdApiUser2 = apiUserRepository.save(apiUser2);
        UUID userId = createdApiUser2.getUserId();

        ApiSubscription apiSubscription3 = new ApiSubscription();
        apiSubscription3.setUserId(userId);
        apiSubscription3.setListType(ListType.CROWN_DAILY_LIST);
        apiSubscription3.setSensitivity(Sensitivity.PUBLIC);

        ApiSubscription apiSubscription4 = new ApiSubscription();
        apiSubscription4.setUserId(userId);
        apiSubscription4.setListType(ListType.CROWN_DAILY_LIST);
        apiSubscription4.setSensitivity(Sensitivity.PRIVATE);

        ApiSubscription apiSubscription5 = new ApiSubscription();
        apiSubscription5.setUserId(userId);
        apiSubscription5.setListType(ListType.CROWN_DAILY_LIST);
        apiSubscription5.setSensitivity(Sensitivity.CLASSIFIED);

        apiSubscriptionRepository.saveAll(List.of(apiSubscription3, apiSubscription4, apiSubscription5));
    }

    @AfterAll
    void shutdown() {
        apiSubscriptionRepository.deleteAll();
        apiUserRepository.deleteAll();
    }

    @Test
    void shouldFindAndDeleteApiSubscriptionsByUserId() {
        List<ApiSubscription> apiSubscriptions = apiSubscriptionRepository.findAllByUserId(userId);

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

        apiSubscriptionRepository.deleteAllByUserId(userId);

        assertThat(apiSubscriptionRepository.findAllByUserId(userId))
            .as("Third-party API subscription should be deleted")
            .isEmpty();
    }

    @Test
    void shouldNotFindApiSubscriptionsByInvalidUser() {
        assertThat(apiSubscriptionRepository.findAllByUserId(INVALID_USER_ID))
            .as("Third-party API subscription should be empty")
            .isEmpty();
    }

    @Test
    void shouldFindApiSubscriptionsByListTypeAndSensitivities() {
        List<ApiSubscription> apiSubscriptions = apiSubscriptionRepository
            .findByListTypeAndSensitivityIn(
                ListType.CROWN_DAILY_LIST,
                List.of(Sensitivity.PUBLIC, Sensitivity.CLASSIFIED)
            );

        assertThat(apiSubscriptions)
            .as("Third-party API subscription count does not match")
            .hasSize(2);

        assertThat(apiSubscriptions)
            .as("Third-party API subscription user IDs do not match")
            .extracting(ApiSubscription::getUserId)
            .containsOnlyOnce(userId);

        assertThat(apiSubscriptions)
            .as("Third-party API subscription list types do not match")
            .extracting(ApiSubscription::getListType)
            .containsOnlyOnce(ListType.CROWN_DAILY_LIST);

        assertThat(apiSubscriptions)
            .as("Third-party API subscription sensitivities do not match")
            .extracting(ApiSubscription::getSensitivity)
            .containsExactlyInAnyOrder(Sensitivity.PUBLIC, Sensitivity.CLASSIFIED);
    }
}

