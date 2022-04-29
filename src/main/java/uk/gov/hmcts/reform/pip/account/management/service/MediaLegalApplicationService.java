package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.hmcts.reform.pip.account.management.database.MediaLegalApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;

public class MediaLegalApplicationService {

    @Autowired
    MediaLegalApplicationRepository mediaLegalApplicationRepository;

    public String createApplication (MediaAndLegalApplication mediaAndLegalApplication){
        mediaLegalApplicationRepository.save(mediaAndLegalApplication);
        return "Success";
    }

}
