package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.helper.MediaLegalApplicationHelper;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplicationDto;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.service.MediaLegalApplicationService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaLegalApplicationControllerTest {
    @Mock
    private MediaLegalApplicationService mediaLegalApplicationService;

    @InjectMocks
    private MediaLegalApplicationController mediaLegalApplicationController;

    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    private final MediaLegalApplicationHelper helper = new MediaLegalApplicationHelper();

    @Test
    void testGetApplications() {
        List<MediaAndLegalApplication> applicationList = helper.createApplicationList(5);

        when(mediaLegalApplicationService.getApplications()).thenReturn(applicationList);

        assertEquals(HttpStatus.OK, mediaLegalApplicationController.getApplications().getStatusCode(),
                     STATUS_CODE_MATCH);

        assertEquals(applicationList, mediaLegalApplicationController.getApplications().getBody(),
                     "Should return list of found applications");
    }

    @Test
    void testGetApplicationsByStatus() {
        List<MediaAndLegalApplication> applicationList = helper.createApplicationList(5);

        when(mediaLegalApplicationService.getApplicationsByStatus(
            MediaLegalApplicationStatus.PENDING)).thenReturn(applicationList);

        assertEquals(HttpStatus.OK, mediaLegalApplicationController.getApplicationsByStatus(
            MediaLegalApplicationStatus.PENDING).getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(applicationList, mediaLegalApplicationController.getApplicationsByStatus(
            MediaLegalApplicationStatus.PENDING).getBody(),
                     "Should return list of found applications");
    }

    @Test
    void testGetApplicationById() {
        MediaAndLegalApplication application = helper.createApplication(MediaLegalApplicationHelper.STATUS);

        when(mediaLegalApplicationService.getApplicationById(helper.TEST_ID)).thenReturn(application);

        assertEquals(HttpStatus.OK, mediaLegalApplicationController.getApplicationById(helper.TEST_ID)
            .getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, mediaLegalApplicationController.getApplicationById(helper.TEST_ID).getBody(),
                     "Should return the correct application");
    }

    @Test
    void testCreateApplication() {
        MediaAndLegalApplicationDto applicationDto = new MediaAndLegalApplicationDto();
        applicationDto.setFullName("Test user");
        applicationDto.setEmail("test@email.com");
        applicationDto.setEmployer("Test employer");
        applicationDto.setStatus(MediaLegalApplicationStatus.PENDING);

        MediaAndLegalApplication application = helper.createApplication(MediaLegalApplicationStatus.PENDING);

        when(mediaLegalApplicationService.createApplication(applicationDto.toEntity(), helper.FILE))
            .thenReturn(application);

        ResponseEntity<MediaAndLegalApplication> response =
            mediaLegalApplicationController.createApplication(applicationDto, helper.FILE);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, response.getBody(), "Should return the expected application");
    }

    @Test
    void testUpdateApplication() {
        MediaAndLegalApplication application = helper.createApplication(MediaLegalApplicationStatus.APPROVED);

        when(mediaLegalApplicationService.updateApplication(helper.TEST_ID, MediaLegalApplicationStatus.APPROVED))
            .thenReturn(application);

        ResponseEntity<MediaAndLegalApplication> response = mediaLegalApplicationController
            .updateApplication(helper.TEST_ID, MediaLegalApplicationStatus.APPROVED);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, response.getBody(), "Should return the expected application");
    }

    @Test
    void testDeleteApplication() {
        ResponseEntity<String> response = mediaLegalApplicationController.deleteApplication(helper.TEST_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals("Application deleted", response.getBody(), "Should return expected deletion message");
    }
}
