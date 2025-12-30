package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ApiUserRepositoryTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String USER_NAME = "Test name";

    @Autowired
    ApiUserRepository apiUserRepository;

    @BeforeEach
    void setup() {
        ApiUser apiUser = new ApiUser();
        apiUser.setUserId(USER_ID);
        apiUser.setName(USER_NAME);
        apiUserRepository.save(apiUser);
    }

    @AfterEach
    void shutdown() {
        apiUserRepository.deleteAll();
    }

    @Test
    void shouldFindApiUserByUserId() {
        Optional<ApiUser> apiUser = apiUserRepository.findByUserId(USER_ID);

        assertThat(apiUser)
            .as("Third-party API user does not match")
            .isPresent()
            .get()
            .extracting(ApiUser::getName)
            .isEqualTo(USER_NAME);
    }

    @Test
    void shouldDeleteApiUserByUserId() {
        apiUserRepository.deleteByUserId(USER_ID);

        assertThat(apiUserRepository.findByUserId(USER_ID))
            .as("Third-party API user should be deleted")
            .isNotPresent();
    }
}

