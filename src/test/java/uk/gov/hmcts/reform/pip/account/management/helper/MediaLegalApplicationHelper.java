package uk.gov.hmcts.reform.pip.account.management.helper;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Helper class for the MediaAndLegalApplication tests.
 */
public final class MediaLegalApplicationHelper {

    public static final MultipartFile FILE = new MockMultipartFile("test", (byte[]) null);
    public static final UUID TEST_ID = UUID.randomUUID();
    private static final String FULL_NAME = "Test User";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    public static final MediaLegalApplicationStatus STATUS = MediaLegalApplicationStatus.PENDING;

    private MediaLegalApplicationHelper() {
        // Default constructor
    }

    public static MediaAndLegalApplication createApplication(MediaLegalApplicationStatus status) {
        MediaAndLegalApplication application = new MediaAndLegalApplication();
        application.setId(TEST_ID);
        application.setFullName(FULL_NAME);
        application.setEmail(EMAIL);
        application.setEmployer(EMPLOYER);
        application.setStatus(status);

        return application;
    }

    public static List<MediaAndLegalApplication> createApplicationList(int numOfApplications) {
        List<MediaAndLegalApplication> applicationList = new ArrayList<>();

        for (int i = 0; i < numOfApplications; i++) {
            applicationList.add(createApplication(STATUS));
        }

        return applicationList;
    }
}
