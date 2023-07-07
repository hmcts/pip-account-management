package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationDto;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.service.MediaApplicationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@Tag(name = "Account Management - API for managing media applications")
@IsAdmin
@RequestMapping("/application")
public class MediaApplicationController {

    public static final String MEDIA_APPLICATION = "{MediaApplication}";
    public static final String NO_MEDIA_APPLICATION_FOUND_WITH_ID = "No media application found with id: {id}";
    private final MediaApplicationService mediaApplicationService;

    private static final String NO_CONTENT_MESSAGE = "The request has been successfully fulfilled";
    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    private static final String AUTH_ERROR_CODE = "403";
    private static final String OK_ERROR_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";


    @Autowired
    public MediaApplicationController(MediaApplicationService mediaApplicationService) {
        this.mediaApplicationService = mediaApplicationService;
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "List<{MediaApplication}>")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Get all applications")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MediaApplication>> getApplications() {
        return ResponseEntity.ok(mediaApplicationService.getApplications());
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "List<{MediaApplication}>")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Get all application by the status")
    @GetMapping(value = "/status/{status}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MediaApplication>> getApplicationsByStatus(
        @PathVariable MediaApplicationStatus status) {
        return ResponseEntity.ok(mediaApplicationService.getApplicationsByStatus(status));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<MediaApplication> getApplicationById(@PathVariable UUID id) {
        return ResponseEntity.ok(mediaApplicationService.getApplicationById(id));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Image with id: {id} is returned")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No image found with id: {id}")
    @GetMapping("/image/{id}")
    public ResponseEntity<Resource> getImageById(@PathVariable String id) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .body(mediaApplicationService.getImageById(id));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = "400", description = "Validation failed message")
    @Operation(summary = "Create a new application")
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaApplication> createApplication(
        @ModelAttribute("application") @Valid MediaApplicationDto application,
        @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mediaApplicationService.createApplication(application.toEntity(), file));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @Operation(summary = "Update an existing application")
    @PutMapping("/{id}/{status}")
    public ResponseEntity<MediaApplication> updateApplication(@PathVariable UUID id,
                                                              @PathVariable MediaApplicationStatus status) {
        return ResponseEntity.ok(
            mediaApplicationService.updateApplication(id, status));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @Operation(summary = "Update a media application, sending an update email alongside")
    @PutMapping(value = "/{id}/{status}/reasons", consumes = APPLICATION_JSON_VALUE)
    public ResponseEntity<MediaApplication> updateApplicationRejection(
        @RequestBody Map<String, List<String>> reasons,
        @PathVariable MediaApplicationStatus status, @PathVariable UUID id) {
        return ResponseEntity.ok(mediaApplicationService.updateApplication(id, status, reasons));
    }


    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Application deleted")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @Operation(summary = "Delete an application")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteApplication(@PathVariable UUID id) {
        mediaApplicationService.deleteApplication(id);
        return ResponseEntity.ok("Application deleted");
    }

    @ApiResponse(responseCode = "204", description = NO_CONTENT_MESSAGE)
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Reports all media applications and deletes approved and rejected applications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/reporting")
    public ResponseEntity<Void> reportApplications() {
        mediaApplicationService.processApplicationsForReporting();
        return ResponseEntity.noContent().build();
    }
}
