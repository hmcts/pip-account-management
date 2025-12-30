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

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiOauthConfigurationRepositoryTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CONFIG_ID = UUID.randomUUID();

    @Autowired
    ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    private ApiOauthConfiguration apiOauthConfiguration;

    @BeforeAll
    void setup() {
        apiOauthConfiguration = new ApiOauthConfiguration();
        apiOauthConfiguration.setId(CONFIG_ID);
        apiOauthConfiguration.setUserId(USER_ID);
        apiOauthConfiguration.setClientIdKey("client-id");
        apiOauthConfigurationRepository.save(apiOauthConfiguration);
    }

    @AfterAll
    void shutdown() {
        apiOauthConfigurationRepository.deleteAll();
    }

    @Test
    void shouldFindApiOauthConfigurationByUserId() {
        Optional<ApiOauthConfiguration> found = apiOauthConfigurationRepository.findByUserId(USER_ID);
        assertThat(found)
            .as("ApiOauthConfiguration should be found by userId")
            .isPresent()
            .get()
            .extracting(ApiOauthConfiguration::getClientIdKey)
            .isEqualTo("client-id");
    }

    @Test
    void shouldDeleteApiOauthConfigurationByUserId() {
        apiOauthConfigurationRepository.deleteByUserId(USER_ID);
        assertThat(apiOauthConfigurationRepository.findByUserId(USER_ID))
            .as("ApiOauthConfiguration should be deleted by userId")
            .isNotPresent();
    }
}

