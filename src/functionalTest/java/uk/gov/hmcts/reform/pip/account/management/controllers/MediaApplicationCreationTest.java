package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.restassured.response.Response;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureBlobConfigurationTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(classes = {Application.class, OAuthClient.class,
    AzureBlobConfigurationTestConfiguration.class, AzureConfigurationClientTestConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MediaApplicationCreationTest extends FunctionalTestBase {

    private static final String TEST_NAME = "E2E Account Management Test Name";
    private static final String TEST_EMPLOYER = "E2E Account Management Test Employer";
    private static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));

    private static final String TEST_EMAIL = String.format(TEST_EMAIL_PREFIX + "@justice.gov.uk");
    private static final String STATUS = "PENDING";

    private static final String TESTING_SUPPORT_APPLICATION_URL = "/testing-support/application/";
    private static final String MEDIA_APPLICATION_URL = "/application";
    private static final String BEARER = "Bearer ";
    private static final String MOCK_FILE = "files/test-image.png";

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_APPLICATION_URL + TEST_EMAIL, Map.of(AUTHORIZATION, BEARER + accessToken), "");
    }

    @Test
    void shouldBeAbleToCreateAMediaApplication() throws Exception {
        final Response response =
            doPostMultipartForApplication(MEDIA_APPLICATION_URL, Map.of(AUTHORIZATION, BEARER + accessToken),
                new ClassPathResource(MOCK_FILE).getFile(), TEST_NAME, TEST_EMAIL, TEST_EMPLOYER, STATUS);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
    }
}
