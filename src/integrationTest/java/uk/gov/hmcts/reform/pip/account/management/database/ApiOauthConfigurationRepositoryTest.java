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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiOauthConfigurationRepositoryTest {

    private static final UUID USER_ID1 = UUID.randomUUID();
    private static final UUID USER_ID2 = UUID.randomUUID();
    private static final UUID CONFIG_ID = UUID.randomUUID();
    private static final String CLIENT_ID_KEY = "TestKey";

    @Autowired
    ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    @BeforeAll
    void setup() {
        ApiOauthConfiguration apiOauthConfiguration1 = new ApiOauthConfiguration();
        apiOauthConfiguration1.setId(CONFIG_ID);
        apiOauthConfiguration1.setUserId(USER_ID1);
        apiOauthConfiguration1.setClientIdKey(CLIENT_ID_KEY);

        ApiOauthConfiguration apiOauthConfiguration2 = new ApiOauthConfiguration();
        apiOauthConfiguration2.setId(CONFIG_ID);
        apiOauthConfiguration2.setUserId(USER_ID2);
        apiOauthConfiguration2.setClientIdKey(CLIENT_ID_KEY);

        apiOauthConfigurationRepository.saveAll(List.of(apiOauthConfiguration1, apiOauthConfiguration2));
    }

    @AfterAll
    void shutdown() {
        apiOauthConfigurationRepository.deleteAll();
    }

    @Test
    void shouldFindApiOauthConfigurationByUserId() {
        Optional<ApiOauthConfiguration> found = apiOauthConfigurationRepository.findByUserId(USER_ID1);
        assertThat(found)
            .as("Third-party API OAuth configuration should be found")
            .isPresent()
            .get()
            .extracting(ApiOauthConfiguration::getClientIdKey)
            .isEqualTo(CLIENT_ID_KEY);
    }

    @Test
    void shouldDeleteApiOauthConfigurationByUserId() {
        assertThat(apiOauthConfigurationRepository.findByUserId(USER_ID2))
            .as("Third-party API OAuth configuration should exist")
            .isPresent();

        apiOauthConfigurationRepository.deleteByUserId(USER_ID2);

        assertThat(apiOauthConfigurationRepository.findByUserId(USER_ID2))
            .as("Third-party API OAuth configuration should be deleted by userId")
            .isNotPresent();
    }
}

