package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

class MediaApplicationCreationTest extends AccountHelperBase {

    private static final String TEST_NAME = "E2E Account Management Test Name";
    private static final String TEST_EMPLOYER = "E2E Account Management Test Employer";
    private static final String TEST_EMAIL = TEST_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String STATUS = "PENDING";
    private static final String MOCK_FILE = "files/test-image.png";

    Map<String, List<String>> reasons =
        Map.of("Reason 1", List.of("Reason 1", "Reason 2"), "Reason 2", List.of("Reason 3", "Reason 4"));


    private Map<String, String> headers;

    @BeforeAll
    public void startUp() throws JsonProcessingException {
        String systemAdminUserId;
        PiUser systemAdminUser = createSystemAdminAccount();
        systemAdminUserId = systemAdminUser.getUserId();

        String adminCtscUserId = getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), Roles.INTERNAL_ADMIN_CTSC,
                          UserProvenances.SSO, systemAdminUserId));

        headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, adminCtscUserId);
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_APPLICATION_URL + TEST_EMAIL, headers);
    }

    private MediaApplication createApplication() throws IOException {
        Response response = doPostMultipartForApplication(MEDIA_APPLICATION_URL, headers,
                                      new ClassPathResource(MOCK_FILE).getFile(),
                                                          TEST_NAME, TEST_EMAIL, TEST_EMPLOYER, STATUS);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        return response.getBody().as(MediaApplication.class);
    }

    @Test
    void shouldBeAbleToCreateAndGetAMediaApplication() throws Exception {
        MediaApplication mediaApplication = createApplication();

        final Response getResponse = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL, mediaApplication.getId()),
                                   headers);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        MediaApplication retrievedMediaApplication = getResponse.getBody().as(MediaApplication.class);

        assertThat(retrievedMediaApplication.getId()).isEqualTo(mediaApplication.getId());
        assertThat(retrievedMediaApplication.getStatus()).isEqualTo(MediaApplicationStatus.PENDING);
    }

    @Test
    void shouldBeAbleToGetTheImageForAnApplication() throws Exception {
        MediaApplication mediaApplication = createApplication();

        final Response getResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  headers);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        byte[] retrievedImage = getResponse.getBody().asByteArray();

        assertThat(retrievedImage).isEqualTo(new ClassPathResource(MOCK_FILE).getContentAsByteArray());
    }

    @Test
    void shouldBeAbleToGetApplicationsByStatus() throws Exception {
        MediaApplication mediaApplication = createApplication();
        MediaApplication approvedMediaApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, approvedMediaApplication.getId()),
                     headers);

        final Response getResponse = doGetRequest(GET_APPLICATIONS_BY_STATUS,
                                                  headers);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        MediaApplication[] retrievedApplications = getResponse.getBody().as(MediaApplication[].class);

        assertThat(retrievedApplications).anyMatch(app -> app.getId().equals(mediaApplication.getId()));
        assertThat(retrievedApplications).noneMatch(app -> app.getId().equals(approvedMediaApplication.getId()));
    }

    @Test
    void shouldBeAbleToGetAllMediaApplications() throws Exception {
        MediaApplication mediaApplication = createApplication();
        MediaApplication approvedMediaApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, approvedMediaApplication.getId()),
                     bearer);

        final Response getResponse = doGetRequest(MEDIA_APPLICATION_URL, bearer);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        MediaApplication[] retrievedApplications = getResponse.getBody().as(MediaApplication[].class);

        assertThat(retrievedApplications).anyMatch(app -> app.getId().equals(mediaApplication.getId()));
        assertThat(retrievedApplications).anyMatch(app -> app.getId().equals(approvedMediaApplication.getId()));
    }

    @Test
    void statusShouldUpdateWhenApplicationIsUpdated() throws Exception {
        MediaApplication mediaApplication = createApplication();

        Response approvedMediaApplication = doPutRequest(String.format(APPROVE_APPLICATION, mediaApplication.getId()),
                     headers);

        assertThat(approvedMediaApplication.getStatusCode()).isEqualTo(OK.value());
        assertThat(approvedMediaApplication.getBody().as(MediaApplication.class).getStatus())
            .isEqualTo(MediaApplicationStatus.APPROVED);

        MediaApplication getApprovedApplication = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL,
                                                                             mediaApplication.getId()), headers)
            .getBody().as(MediaApplication.class);

        assertThat(getApprovedApplication.getStatus()).isEqualTo(MediaApplicationStatus.APPROVED);
    }

    @Test
    void shouldNoLongerBeAbleToGetImageWhenApplicationIsApproved() throws Exception {
        MediaApplication mediaApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, mediaApplication.getId()),
                     headers);

        final Response getResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  headers);

        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldNoLongerBeAbleToGetImageWhenApplicationIsRejected() throws Exception {
        MediaApplication mediaApplication = createApplication();

        doPutRequest(String.format(REJECT_APPLICATION, mediaApplication.getId()),
                     headers);

        final Response getResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  headers);

        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAnApplication() throws Exception {
        MediaApplication mediaApplication = createApplication();

        Response deleteResponse = doDeleteRequest(String.format(GET_MEDIA_APPLICATION_URL, mediaApplication.getId()),
                     headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        final Response getImageResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                  headers);

        assertThat(getImageResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());

        final Response getApplicationResponse = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL,
                                                                           mediaApplication.getId()), headers);

        assertThat(getApplicationResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToUpdateAnApplicationWithRejectionReasons() throws Exception {
        MediaApplication mediaApplication = createApplication();

        Response rejectedApplicationResponse =
            doPutRequestWithBody(String.format(REJECT_APPLICATION_WITH_REASONS, mediaApplication.getId()),
                     headers, new ObjectMapper().writeValueAsString(reasons));

        assertThat(rejectedApplicationResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(rejectedApplicationResponse.getBody().as(MediaApplication.class).getStatus())
            .isEqualTo(MediaApplicationStatus.REJECTED);

        MediaApplication getRejectedApplication = doGetRequest(String.format(GET_MEDIA_APPLICATION_URL,
                                                                             mediaApplication.getId()), headers)
            .getBody().as(MediaApplication.class);

        assertThat(getRejectedApplication.getStatus()).isEqualTo(MediaApplicationStatus.REJECTED);
    }

    @Test
    void shouldNoLongerBeAbleToGetImageWhenApplicationIsRejectedWithReasons() throws Exception {
        MediaApplication mediaApplication = createApplication();

        doPutRequestWithBody(String.format(REJECT_APPLICATION_WITH_REASONS, mediaApplication.getId()),
                                     headers,
                                 new ObjectMapper().writeValueAsString(reasons));

        final Response getImageResponse = doGetRequest(String.format(GET_IMAGE_BY_ID, mediaApplication.getImage()),
                                                       headers);

        assertThat(getImageResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void ensureMediaApplicationsAreDeletedWhenReportingIsCalled() throws Exception {
        MediaApplication rejectedApplication = createApplication();

        doPutRequest(String.format(REJECT_APPLICATION, rejectedApplication.getId()),
                     headers);

        MediaApplication approvedApplication = createApplication();

        doPutRequest(String.format(APPROVE_APPLICATION, approvedApplication.getId()),
                     headers);

        MediaApplication pendingApplication = createApplication();

        Response reportingResponse = doPostRequest(REPORTING, headers, "");
        assertThat(reportingResponse.getStatusCode()).isEqualTo(NO_CONTENT.value());

        final Response getResponse = doGetRequest(MEDIA_APPLICATION_URL, headers);

        MediaApplication[] retrievedApplications = getResponse.getBody().as(MediaApplication[].class);

        assertThat(retrievedApplications).anyMatch(app -> app.getId().equals(pendingApplication.getId()));
        assertThat(retrievedApplications).noneMatch(app -> app.getId().equals(rejectedApplication.getId()));
        assertThat(retrievedApplications).noneMatch(app -> app.getId().equals(approvedApplication.getId()));
    }
}
