package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.service.MediaLegalApplicationService;

import java.util.List;

import static org.springframework.http.MediaType.*;

@RestController
@Api(tags = "Account Management - API for managing media & orphaned legal professional applications")
@RequestMapping("/application")
public class MediaLegalApplicationController {

    private MediaLegalApplicationService mediaLegalApplicationService;

    @Autowired
    public MediaLegalApplicationController(MediaLegalApplicationService mediaLegalApplicationService) {
        this.mediaLegalApplicationService = mediaLegalApplicationService;
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "List<{MediaAndLegalApplication}>"),
    })
    @ApiOperation("Get all applications")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MediaAndLegalApplication>> getApplications() {
        return ResponseEntity.ok(mediaLegalApplicationService.getApplications());
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "List<{MediaAndLegalApplication}>"),
    })
    @ApiOperation("Get all application by the status")
    @GetMapping(value = "/{status}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MediaAndLegalApplication>> getApplicationsByStatus(@PathVariable MediaLegalApplicationStatus status) {
        return ResponseEntity.ok(mediaLegalApplicationService.getApplicationsByStatus(status));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "{MediaAndLegalApplication}"),
    })
    @ApiOperation("Create a new application")
    @PostMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<MediaAndLegalApplication> createApplication(
        MediaAndLegalApplication application,
        @RequestPart(value = "file") MultipartFile file) {
        return ResponseEntity.ok(mediaLegalApplicationService.createApplication(application, file));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "{MediaAndLegalApplication}"),
    })
    @ApiOperation("Update an existing application")
    @PutMapping
    public ResponseEntity<MediaAndLegalApplication> updateApplication(
        MediaAndLegalApplication application,
        @RequestPart(value = "file", required = false) MultipartFile file){
        return ResponseEntity.ok(mediaLegalApplicationService.updateApplication(application, file));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Application deleted"),
    })
    @ApiOperation("Delete an application")
    @DeleteMapping
    public ResponseEntity<String> deleteApplication(MediaAndLegalApplication application) {
        mediaLegalApplicationService.deleteApplication(application);
        return ResponseEntity.ok("Application deleted");
    }
}
