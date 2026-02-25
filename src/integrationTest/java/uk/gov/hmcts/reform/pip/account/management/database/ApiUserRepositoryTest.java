package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUserStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiUserRepositoryTest {
    private static final UUID INVALID_USER_ID = UUID.randomUUID();
    private static final String USER_NAME = "Test name";

    @Autowired
    ApiUserRepository apiUserRepository;

    @AfterAll
    void shutdown() {
        apiUserRepository.deleteAll();
    }

    @Test
    void shouldFindAndDeleteApiUserByUserId() {
        ApiUser apiUser = new ApiUser();
        apiUser.setName(USER_NAME);
        ApiUser createdApiUser = apiUserRepository.save(apiUser);
        UUID userId = createdApiUser.getUserId();

        assertThat(apiUserRepository.findByUserId(userId))
            .as("Third-party API user does not match")
            .isPresent()
            .get()
            .extracting(ApiUser::getName)
            .isEqualTo(USER_NAME);

        apiUserRepository.deleteById(userId);

        assertThat(apiUserRepository.findByUserId(userId))
            .as("Third-party API user should be deleted")
            .isNotPresent();
    }

    @Test
    void shouldNotFindApiUserByInvalidUserId() {
        assertThat(apiUserRepository.findByUserId(INVALID_USER_ID))
            .as("Third-party API user should not exist")
            .isNotPresent();
    }

    @Test
    void shouldSetDefaultStatusToPendingWhenApiUserIsCreated() {
        ApiUser apiUser = new ApiUser();
        apiUser.setName(USER_NAME);
        ApiUser createdApiUser = apiUserRepository.save(apiUser);

        assertThat(createdApiUser.getStatus())
            .as("Default status should be PENDING")
            .isEqualTo(ApiUserStatus.PENDING);
    }

}

