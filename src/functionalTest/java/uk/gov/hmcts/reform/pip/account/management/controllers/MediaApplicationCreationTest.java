package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
@SuppressWarnings("PMD.TooManyMethods")
class MediaApplicationCreationTest extends FunctionalTestBase {

    private static final String TEST_NAME = "E2E Account Management Test Name";
    private static final String TEST_EMPLOYER = "E2E Account Management Test Employer";
    private static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));

    private static final String TEST_EMAIL = String.format(TEST_EMAIL_PREFIX + "@justice.gov.uk");
    private static final String STATUS = "PENDING";

    private static final String TESTING_SUPPORT_APPLICATION_URL = "/testing-support/application/";
    private static final String MEDIA_APPLICATION_URL = "/application";
    private static final String GET_IMAGE_BY_ID = "/application/image/%s";
    private static final String GET_MEDIA_APPLICATION_URL = "/application/%s";
    private static final String APPROVE_APPLICATION = "/application/%s/APPROVED";
    private static final String REJECT_APPLICATION = "/application/%s/REJECTED";
    private static final String REJECT_APPLICATION_WITH_REASONS = "/application/%s/REJECTED/reasons";
    private static final String GET_APPLICATIONS_BY_STATUS = "/application/status/PENDING";
    private static final String REPORTING = "/application/reporting";
    private static final String GET_ALL_APPLICATIONS = "/application";
    private static final String BEARER = "Bearer ";
    private static final String MOCK_FILE = "files/test-image.png";

    Map<String, List<String>> reasons =
        Map.of("Reason 1", List.of("Reason 1", "Reason 2"), "Reason 2", List.of("Reason 3", "Reason 4"));

    private Map<String, String> bearer;

    @BeforeAll
    public void startUp() {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_APPLICATION_URL + TEST_EMAIL, bearer);
    }

    private MediaApplication createApplication() throws IOException {
        Response response = doPostMultipartForApplication(MEDIA_APPLICATION_URL, bearer,
                                      new ClassPathResource(MOCK_FILE).getFile(),
                                                          TEST_NAME, TEST_EMAIL, TEST_EMPLOYER, STATUS);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        return response.getBody().as(MediaApplication.class);
    }

    @Test
    void shouldBeAbleToCreateAndGetAMediaApplication() throws Exception {
        MediaApplication mediaApplication = createApplication();

        final Response getResponse = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL, mediaApplication.getId()),
                                   bearer);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        MediaApplication retrievedMediaApplication = getResponse.getBody().as(MediaApplication.class);

        assertThat(retrievedMediaApplication.getId()).isEqualTo(mediaApplication.getId());
        assertThat(retrievedMediaApplication.getStatus()).isEqualTo(MediaApplicationStatus.PENDING);
    }

    @Test
    void shouldBeAbleToGetTheImageForAnApplication() throws Exception {
        MediaApplication mediaApplication = createApplication();

        final Response getResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  bearer);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        byte[] retrievedImage = getResponse.getBody().asByteArray();

        assertThat(retrievedImage).isEqualTo(new ClassPathResource(MOCK_FILE).getContentAsByteArray());
    }

    @Test
    void shouldBeAbleToGetApplicationsByStatus() throws Exception {
        MediaApplication mediaApplication = createApplication();
        MediaApplication approvedMediaApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, approvedMediaApplication.getId()),
                     bearer);

        final Response getResponse = doGetRequest(GET_APPLICATIONS_BY_STATUS,
                                                  bearer);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        MediaApplication[] retrievedApplciations = getResponse.getBody().as(MediaApplication[].class);

        assertThat(retrievedApplciations).anyMatch(app -> app.getId().equals(mediaApplication.getId()));
        assertThat(retrievedApplciations).noneMatch(app -> app.getId().equals(approvedMediaApplication.getId()));
    }

    @Test
    void shouldBeAbleToGetAllMediaApplications() throws Exception {
        MediaApplication mediaApplication = createApplication();
        MediaApplication approvedMediaApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, approvedMediaApplication.getId()),
                     bearer);

        final Response getResponse = doGetRequest(GET_ALL_APPLICATIONS,
                                                  bearer);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        MediaApplication[] retrievedApplications = getResponse.getBody().as(MediaApplication[].class);

        assertThat(retrievedApplications).anyMatch(app -> app.getId().equals(mediaApplication.getId()));
        assertThat(retrievedApplications).anyMatch(app -> app.getId().equals(approvedMediaApplication.getId()));
    }

    @Test
    void statusShouldUpdateWhenApplicationIsUpdated() throws Exception {
        MediaApplication mediaApplication = createApplication();

        Response approvedMediaApplication = doPutRequest(String.format(APPROVE_APPLICATION, mediaApplication.getId()),
                     bearer);

        assertThat(approvedMediaApplication.getStatusCode()).isEqualTo(OK.value());
        assertThat(approvedMediaApplication.getBody().as(MediaApplication.class).getStatus())
            .isEqualTo(MediaApplicationStatus.APPROVED);

        MediaApplication getApprovedApplication = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL,
                                                                             mediaApplication.getId()), bearer)
            .getBody().as(MediaApplication.class);

        assertThat(getApprovedApplication.getStatus()).isEqualTo(MediaApplicationStatus.APPROVED);
    }

    @Test
    void shouldNoLongerBeAbleToGetImageWhenApplicationIsApproved() throws Exception {
        MediaApplication mediaApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, mediaApplication.getId()),
                     bearer);

        final Response getResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  bearer);

        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldNoLongerBeAbleToGetImageWhenApplicationIsRejected() throws Exception {
        MediaApplication mediaApplication = createApplication();

        doPutRequest(String.format(REJECT_APPLICATION, mediaApplication.getId()),
                     bearer);

        final Response getResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  bearer);

        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAnApplication() throws Exception {
        MediaApplication mediaApplication = createApplication();

        Response deleteResponse = doDeleteRequest(String.format(GET_MEDIA_APPLICATION_URL, mediaApplication.getId()),
                     bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        final Response getImageResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  bearer);

        assertThat(getImageResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());

        final Response getApplicationResponse = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL,
                                                                           mediaApplication.getId()), bearer);

        assertThat(getApplicationResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToUpdateAnApplicationWithRejectionReasons() throws Exception {
        MediaApplication mediaApplication = createApplication();

        Response rejectedApplicationResponse =
            doPutRequestWithJsonBody(String.format(REJECT_APPLICATION_WITH_REASONS, mediaApplication.getId()),
                     bearer, new ObjectMapper().writeValueAsString(reasons));

        assertThat(rejectedApplicationResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(rejectedApplicationResponse.getBody().as(MediaApplication.class).getStatus())
            .isEqualTo(MediaApplicationStatus.REJECTED);

        MediaApplication getRejectedApplication = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL,
                                                                             mediaApplication.getId()), bearer)
            .getBody().as(MediaApplication.class);

        assertThat(getRejectedApplication.getStatus()).isEqualTo(MediaApplicationStatus.REJECTED);
    }

    @Test
    void shouldNoLongerBeAbleToGetImageWhenApplicationIsRejectedWithReasons() throws Exception {
        MediaApplication mediaApplication = createApplication();

        doPutRequestWithJsonBody(String.format(REJECT_APPLICATION_WITH_REASONS, mediaApplication.getId()),
                                     bearer,
                                 new ObjectMapper().writeValueAsString(reasons));

        final Response getImageResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                       bearer);

        assertThat(getImageResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void ensureMediaApplicationsAreDeletedWhenReportingIsCalled() throws Exception {
        MediaApplication rejectedApplication = createApplication();

        doPutRequest(String.format(REJECT_APPLICATION, rejectedApplication.getId()),
                     bearer);

        MediaApplication approvedApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, approvedApplication.getId()),
                     bearer);

        MediaApplication pendingApplication = createApplication();

        Response reportingResponse = doPostRequest(REPORTING, bearer, "");
        assertThat(reportingResponse.getStatusCode()).isEqualTo(NO_CONTENT.value());

        final Response getResponse = doGetRequest(GET_ALL_APPLICATIONS,
                                                  bearer);

        MediaApplication[] retrievedApplications = getResponse.getBody().as(MediaApplication[].class);

        assertThat(retrievedApplications).anyMatch(app -> app.getId().equals(pendingApplication.getId()));
        assertThat(retrievedApplications).noneMatch(app -> app.getId().equals(rejectedApplication.getId()));
        assertThat(retrievedApplications).noneMatch(app -> app.getId().equals(approvedApplication.getId()));
    }
}
