package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplicationDto;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.service.MediaLegalApplicationService;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@Api(tags = "Account Management - API for managing media & orphaned legal professional applications")
@IsAdmin
@RequestMapping("/application")
public class MediaLegalApplicationController {

    private final MediaLegalApplicationService mediaLegalApplicationService;

    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    @Autowired
    public MediaLegalApplicationController(MediaLegalApplicationService mediaLegalApplicationService) {
        this.mediaLegalApplicationService = mediaLegalApplicationService;
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "List<{MediaAndLegalApplication}>"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
    })
    @ApiOperation("Get all applications")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MediaAndLegalApplication>> getApplications() {
        return ResponseEntity.ok(mediaLegalApplicationService.getApplications());
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "List<{MediaAndLegalApplication}>"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
    })
    @ApiOperation("Get all application by the status")
    @GetMapping(value = "/status/{status}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MediaAndLegalApplication>> getApplicationsByStatus(
        @PathVariable MediaLegalApplicationStatus status) {
        return ResponseEntity.ok(mediaLegalApplicationService.getApplicationsByStatus(status));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "{MediaAndLegalApplication}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(code = 404, message = "No media application found with id: {id}"),
    })
    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<MediaAndLegalApplication> getApplicationById(@PathVariable UUID id) {
        return ResponseEntity.ok(mediaLegalApplicationService.getApplicationById(id));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Image with id: {id} is returned"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(code = 404, message = "No image found with id: {id}"),
    })
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImageById(@PathVariable String id) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .body(mediaLegalApplicationService.getImageById(id));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "{MediaAndLegalApplication}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
    })
    @ApiOperation("Create a new application")
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaAndLegalApplication> createApplication(
        @ModelAttribute("application") MediaAndLegalApplicationDto application,
        @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mediaLegalApplicationService.createApplication(application.toEntity(), file));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "{MediaAndLegalApplication}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(code = 404, message = "No media application found with id: {id}"),
    })
    @ApiOperation("Update an existing application")
    @PutMapping("/{id}/{status}")
    public ResponseEntity<MediaAndLegalApplication> updateApplication(@PathVariable UUID id,
        @PathVariable MediaLegalApplicationStatus status) {
        return ResponseEntity.ok(
            mediaLegalApplicationService.updateApplication(id, status));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Application deleted"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(code = 404, message = "No media application found with id: {id}"),
    })
    @ApiOperation("Delete an application")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteApplication(@PathVariable UUID id) {
        mediaLegalApplicationService.deleteApplication(id);
        return ResponseEntity.ok("Application deleted");
    }
}
