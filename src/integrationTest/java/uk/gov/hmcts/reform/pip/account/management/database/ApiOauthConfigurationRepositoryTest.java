package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiOauthConfigurationRepositoryTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID INVALID_USER_ID = UUID.randomUUID();
    private static final UUID CONFIG_ID = UUID.randomUUID();
    private static final String CLIENT_ID_KEY = "TestKey";

    @Autowired
    ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    @AfterAll
    void shutdown() {
        apiOauthConfigurationRepository.deleteAll();
    }

    @Test
    void shouldFindApiOauthConfigurationByUserId() {
        ApiOauthConfiguration apiOauthConfiguration = new ApiOauthConfiguration();
        apiOauthConfiguration.setId(CONFIG_ID);
        apiOauthConfiguration.setUserId(USER_ID);
        apiOauthConfiguration.setClientIdKey(CLIENT_ID_KEY);
        apiOauthConfigurationRepository.save(apiOauthConfiguration);

        Optional<ApiOauthConfiguration> foundApiOauthConfiguration = apiOauthConfigurationRepository.findByUserId(USER_ID);

        assertThat(foundApiOauthConfiguration)
            .as("Third-party API OAuth configuration should be found")
            .isPresent()
            .get()
            .extracting(ApiOauthConfiguration::getClientIdKey)
            .isEqualTo(CLIENT_ID_KEY);

        apiOauthConfigurationRepository.deleteByUserId(USER_ID);

        assertThat(apiOauthConfigurationRepository.findByUserId(USER_ID))
            .as("Third-party API OAuth configuration should be deleted")
            .isNotPresent();
    }

    @Test
    void shouldNotFindApiOauthConfigurationByINvalidUserId() {
        assertThat(apiOauthConfigurationRepository.findByUserId(INVALID_USER_ID))
            .as("Third-party API OAuth configuration should not exist")
            .isNotPresent();
    }
}

