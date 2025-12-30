package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiUserRepositoryTest {
    private static final UUID USER_ID1 = UUID.randomUUID();
    private static final UUID INVALID_USER_ID = UUID.randomUUID();
    private static final String USER_NAME = "Test name";

    @Autowired
    ApiUserRepository apiUserRepository;

    @BeforeAll
    void setup() {
        ApiUser apiUser1 = new ApiUser();
        apiUser1.setUserId(USER_ID1);
        apiUser1.setName(USER_NAME);
        apiUserRepository.save(apiUser1);
    }

    @AfterAll
    void shutdown() {
        apiUserRepository.deleteAll();
    }

    @Test
    void shouldFindAndDeleteApiUserByUserId() {
        Optional<ApiUser> apiUser = apiUserRepository.findByUserId(USER_ID1);

        assertThat(apiUser)
            .as("Third-party API user does not match")
            .isPresent()
            .get()
            .extracting(ApiUser::getName)
            .isEqualTo(USER_NAME);

        apiUserRepository.deleteByUserId(USER_ID1);

        assertThat(apiUserRepository.findByUserId(USER_ID1))
            .as("Third-party API user should be deleted")
            .isNotPresent();
    }

    @Test
    void shouldNotFindApiUserByInvalidUserId() {
        assertThat(apiUserRepository.findByUserId(INVALID_USER_ID))
            .as("Third-party API user should not exist")
            .isNotPresent();
    }
}

