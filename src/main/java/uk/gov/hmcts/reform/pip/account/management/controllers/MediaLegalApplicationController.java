package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.annotations.Api;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.service.MediaLegalApplicationService;

@RestController
@Api(tags = "Media Application Management - API for managing media & orphaned legal professional applications")
@RequestMapping("/application")
public class MediaLegalApplicationController {

    private MediaLegalApplicationService mediaLegalApplicationService;

    @PostMapping()
    public ResponseEntity<String> createApplication(MediaAndLegalApplication application){
        return ResponseEntity.ok(mediaLegalApplicationService.createApplication(application));
    }

    @PutMapping()
    public ResponseEntity<String> updateApplication(MediaAndLegalApplication application){

    }

    @DeleteMapping()
    public ResponseEntity<String> deleteApplication(MediaAndLegalApplication application){

    }
}
