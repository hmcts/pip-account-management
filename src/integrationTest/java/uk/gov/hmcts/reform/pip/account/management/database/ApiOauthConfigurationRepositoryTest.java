package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiOauthConfigurationRepositoryTest {
    private static final UUID INVALID_USER_ID = UUID.randomUUID();
    private static final String CLIENT_ID_KEY = "TestKey";
    private static final String USER_NAME = "Test name";

    private UUID userId;

    @Autowired
    ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    @Autowired
    ApiUserRepository apiUserRepository;

    @BeforeAll
    void setup() {
        ApiUser apiUser = new ApiUser();
        apiUser.setName(USER_NAME);
        ApiUser createdApiUser = apiUserRepository.save(apiUser);
        userId = createdApiUser.getUserId();

        ApiOauthConfiguration apiOauthConfiguration = new ApiOauthConfiguration();
        apiOauthConfiguration.setUserId(userId);
        apiOauthConfiguration.setClientIdKey(CLIENT_ID_KEY);

        apiOauthConfigurationRepository.save(apiOauthConfiguration);
    }

    @AfterAll
    void shutdown() {
        apiOauthConfigurationRepository.deleteAll();
        apiUserRepository.deleteAll();
    }

    @Test
    void shouldFindApiOauthConfigurationByUserId() {
        Optional<ApiOauthConfiguration> foundApiOauthConfiguration = apiOauthConfigurationRepository
            .findByUserId(userId);

        assertThat(foundApiOauthConfiguration)
            .as("Third-party API OAuth configuration should be found")
            .isPresent()
            .get()
            .extracting(ApiOauthConfiguration::getClientIdKey)
            .isEqualTo(CLIENT_ID_KEY);

        apiOauthConfigurationRepository.deleteByUserId(userId);

        assertThat(apiOauthConfigurationRepository.findByUserId(userId))
            .as("Third-party API OAuth configuration should be deleted")
            .isNotPresent();
    }

    @Test
    void shouldNotFindApiOauthConfigurationByInvalidUserId() {
        assertThat(apiOauthConfigurationRepository.findByUserId(INVALID_USER_ID))
            .as("Third-party API OAuth configuration should not exist")
            .isNotPresent();
    }
}

