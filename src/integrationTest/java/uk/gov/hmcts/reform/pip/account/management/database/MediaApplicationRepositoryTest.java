package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MediaApplicationRepositoryTest {
    private static final String NAME1 = "Test name 1";
    private static final String NAME2 = "Test name 2";
    private static final String NAME3 = "Test name 3";
    private static final String NAME4 = "Test name 4";
    private static final String EMAIL1 = "TestUser1@justice.gov.uk";
    private static final String EMAIL2 = "TestUser2@justice.gov.uk";
    private static final String EMAIL3 = "testuser3@justice.gov.uk";
    private static final String EMAIL4 = "AnotherTestUser@justice.gov.uk";
    private static final String EMPLOYER = "Test Employer";

    private static final String MEDIA_APPLICATION_MATCHED_MESSAGE = "Media application does not match";
    private static final String MEDIA_APPLICATION_EMPTY_MESSAGE = "Media application is not empty";

    @Autowired
    MediaApplicationRepository mediaApplicationRepository;

    @BeforeAll
    void setup() {
        MediaApplication mediaApplication1 = new MediaApplication();
        mediaApplication1.setFullName(NAME1);
        mediaApplication1.setEmail(EMAIL1);
        mediaApplication1.setEmployer(EMPLOYER);
        mediaApplication1.setStatus(MediaApplicationStatus.PENDING);

        MediaApplication mediaApplication2 = new MediaApplication();
        mediaApplication2.setFullName(NAME2);
        mediaApplication2.setEmail(EMAIL2);
        mediaApplication2.setEmployer(EMPLOYER);
        mediaApplication2.setStatus(MediaApplicationStatus.APPROVED);

        MediaApplication mediaApplication3 = new MediaApplication();
        mediaApplication3.setFullName(NAME3);
        mediaApplication3.setEmail(EMAIL3);
        mediaApplication3.setEmployer(EMPLOYER);
        mediaApplication3.setStatus(MediaApplicationStatus.REJECTED);

        MediaApplication mediaApplication4 = new MediaApplication();
        mediaApplication4.setFullName(NAME4);
        mediaApplication4.setEmail(EMAIL4);
        mediaApplication4.setEmployer(EMPLOYER);
        mediaApplication4.setStatus(MediaApplicationStatus.PENDING);

        mediaApplicationRepository.saveAll(
            List.of(mediaApplication1, mediaApplication2, mediaApplication3, mediaApplication4)
        );
    }

    @AfterAll
    void shutdown() {
        mediaApplicationRepository.deleteAll();
    }

    @Test
    void shouldFindAllMediaApplicationsByEmailStartingWithPrefix() {
        assertThat(mediaApplicationRepository.findAllByEmailStartingWithIgnoreCase("testUser"))
            .as(MEDIA_APPLICATION_MATCHED_MESSAGE)
            .hasSize(3)
            .extracting(MediaApplication::getEmail)
            .containsExactlyInAnyOrder(EMAIL1, EMAIL2, EMAIL3);
    }

    @Test
    void shouldFindAllMediaApplicationsByEmailIfPrefixNotMatched() {
        assertThat(mediaApplicationRepository.findAllByEmailStartingWithIgnoreCase("InvalidPrefix"))
            .as(MEDIA_APPLICATION_EMPTY_MESSAGE)
            .isEmpty();
    }
}
