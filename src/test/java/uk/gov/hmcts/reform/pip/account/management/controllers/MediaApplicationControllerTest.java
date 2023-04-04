package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationDto;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.service.MediaApplicationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.FILE;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.STATUS;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.TEST_ID;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.createApplication;
import static uk.gov.hmcts.reform.pip.account.management.helper.MediaApplicationHelper.createApplicationList;

@ExtendWith(MockitoExtension.class)
class MediaApplicationControllerTest {

    @Mock
    private MediaApplicationService mediaApplicationService;

    @InjectMocks
    private MediaApplicationController mediaApplicationController;

    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Test
    void testGetApplications() {
        List<MediaApplication> applicationList = createApplicationList(5);

        when(mediaApplicationService.getApplications()).thenReturn(applicationList);

        assertEquals(HttpStatus.OK, mediaApplicationController.getApplications().getStatusCode(),
                     STATUS_CODE_MATCH);

        assertEquals(applicationList, mediaApplicationController.getApplications().getBody(),
                     "Should return list of found applications");
    }

    @Test
    void testGetApplicationsByStatus() {
        List<MediaApplication> applicationList = createApplicationList(5);

        when(mediaApplicationService.getApplicationsByStatus(
            MediaApplicationStatus.PENDING)).thenReturn(applicationList);

        assertEquals(HttpStatus.OK, mediaApplicationController.getApplicationsByStatus(
            MediaApplicationStatus.PENDING).getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(applicationList, mediaApplicationController.getApplicationsByStatus(
                         MediaApplicationStatus.PENDING).getBody(),
                     "Should return list of found applications");
    }

    @Test
    void testGetApplicationById() {
        MediaApplication application = createApplication(STATUS);

        when(mediaApplicationService.getApplicationById(TEST_ID)).thenReturn(application);

        assertEquals(HttpStatus.OK, mediaApplicationController.getApplicationById(TEST_ID)
            .getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, mediaApplicationController.getApplicationById(TEST_ID).getBody(),
                     "Should return the correct application");
    }

    @Test
    void testCreateApplication() {
        MediaApplicationDto applicationDto = new MediaApplicationDto();
        applicationDto.setFullName("Test user");
        applicationDto.setEmail("test@email.com");
        applicationDto.setEmployer("Test employer");
        applicationDto.setStatus("PENDING");

        MediaApplication application = createApplication(MediaApplicationStatus.PENDING);

        when(mediaApplicationService.createApplication(applicationDto.toEntity(), FILE))
            .thenReturn(application);

        ResponseEntity<MediaApplication> response =
            mediaApplicationController.createApplication(applicationDto, FILE);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, response.getBody(), "Should return the expected application");
    }

    @Test
    void testUpdateApplication() {
        MediaApplication application = createApplication(MediaApplicationStatus.APPROVED);

        when(mediaApplicationService.updateApplication(TEST_ID, MediaApplicationStatus.APPROVED))
            .thenReturn(application);

        ResponseEntity<MediaApplication> response = mediaApplicationController
            .updateApplication(TEST_ID, MediaApplicationStatus.APPROVED);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, response.getBody(), "Should return the expected application");
    }

    @Test
    void testDeleteApplication() {
        ResponseEntity<String> response = mediaApplicationController.deleteApplication(TEST_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals("Application deleted", response.getBody(), "Should return expected deletion message");
    }

    @Test
    void testReportApplications() {
        doNothing().when(mediaApplicationService).processApplicationsForReporting();
        assertThat(mediaApplicationController.reportApplications().getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void testRejectApplicationWithReasons() {
        MediaApplication mediaApplication = new MediaApplication();
        mediaApplication.setFullName("Test Name");

        UUID testUuid = UUID.randomUUID();

        Map<String, List<String>> reasons = new ConcurrentHashMap<>();
        reasons.put("Reason A", List.of("Text A", "Text B"));

        when(mediaApplicationService.updateApplication(testUuid, MediaApplicationStatus.REJECTED, reasons))
            .thenReturn(mediaApplication);

        ResponseEntity<MediaApplication> response =
            mediaApplicationController.updateApplicationRejection(reasons, MediaApplicationStatus.REJECTED, testUuid);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(mediaApplication, response.getBody(), "Returned media application does not match");
    }
}
