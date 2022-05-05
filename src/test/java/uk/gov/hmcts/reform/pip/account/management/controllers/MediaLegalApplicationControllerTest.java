package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.service.MediaLegalApplicationService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MediaLegalApplicationControllerTest {

    private static final String STATUS_CODE_MATCH = "Status code responses should match";
    private static final MultipartFile FILE = new MockMultipartFile("test", (byte[]) null);

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private MediaLegalApplicationService mediaLegalApplicationService;

    @InjectMocks
    private MediaLegalApplicationController mediaLegalApplicationController;

    @Test
    void testGetApplications() {
        List<MediaAndLegalApplication> applicationList = createApplicationList(5);

        when(mediaLegalApplicationService.getApplications()).thenReturn(applicationList);

        assertEquals(HttpStatus.OK, mediaLegalApplicationController.getApplications().getStatusCode(),
                     STATUS_CODE_MATCH);

        assertEquals(applicationList, mediaLegalApplicationController.getApplications().getBody(),
                     "Should return list of found applications");
    }

    @Test
    void testGetApplicationsByStatus() {
        List<MediaAndLegalApplication> applicationList = createApplicationList(5);

        when(mediaLegalApplicationService.getApplicationsByStatus(
            MediaLegalApplicationStatus.PENDING)).thenReturn(applicationList);

        assertEquals(HttpStatus.OK, mediaLegalApplicationController.getApplicationsByStatus(
            MediaLegalApplicationStatus.PENDING).getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(applicationList, mediaLegalApplicationController.getApplicationsByStatus(
            MediaLegalApplicationStatus.PENDING).getBody(),
                     "Should return list of found applications");
    }

    @Test
    void testCreateApplication() {
        MediaAndLegalApplication application = createApplication(MediaLegalApplicationStatus.PENDING);

        when(mediaLegalApplicationService.createApplication(application, FILE)).thenReturn(application);

        ResponseEntity<MediaAndLegalApplication> response =
            mediaLegalApplicationController.createApplication(application, FILE);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, response.getBody(), "Should return the expected application");
    }

    // Test updateApplication
    @Test
    void testUpdateApplication() {
        MediaAndLegalApplication application = createApplication(MediaLegalApplicationStatus.APPROVED);

        when(mediaLegalApplicationService.updateApplication(ID, MediaLegalApplicationStatus.APPROVED))
            .thenReturn(application);

        ResponseEntity<MediaAndLegalApplication> response = mediaLegalApplicationController
            .updateApplication(ID, MediaLegalApplicationStatus.APPROVED);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals(application, response.getBody(), "Should return the expected application");
    }

    @Test
    void testDeleteApplication() {
        ResponseEntity<String> response = mediaLegalApplicationController.deleteApplication(ID);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);

        assertEquals("Application deleted", response.getBody(), "Should return expected deletion message");
    }

    MediaAndLegalApplication createApplication(MediaLegalApplicationStatus status) {
        MediaAndLegalApplication application = new MediaAndLegalApplication();
        application.setFullName("Test User");
        application.setEmail("test@email.com");
        application.setEmployer("Test employer");
        application.setStatus(status);

        return application;
    }

    List<MediaAndLegalApplication> createApplicationList(int numberOfApplications) {
        List<MediaAndLegalApplication> applicationList = new ArrayList<>();

        for (int i = 0; i < numberOfApplications; i++) {
            applicationList.add(createApplication(MediaLegalApplicationStatus.PENDING));
        }

        return applicationList;
    }
}
