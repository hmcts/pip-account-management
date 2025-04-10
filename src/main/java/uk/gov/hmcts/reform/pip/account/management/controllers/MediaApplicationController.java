package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
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
@RequestMapping("/application")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MediaApplicationController {

    private static final String REQUESTER_ID = "x-requester-id";

    public static final String MEDIA_APPLICATION = "{MediaApplication}";
    public static final String NO_MEDIA_APPLICATION_FOUND_WITH_ID = "No media application found with id: {id}";
    private final MediaApplicationService mediaApplicationService;

    private static final String NO_CONTENT_MESSAGE = "The request has been successfully fulfilled";
    private static final String OK_ERROR_CODE = "200";
    private static final String FORBIDDEN_ERROR_CODE = "401";
    private static final String NOT_FOUND_ERROR_CODE = "404";


    @ApiResponse(responseCode = OK_ERROR_CODE, description = "List<{MediaApplication}>")
    @Operation(summary = "Get all applications")
    @GetMapping(produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<MediaApplication>> getApplications() {
        return ResponseEntity.ok(mediaApplicationService.getApplications());
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "List<{MediaApplication}>")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "Action forbidden")
    @Operation(summary = "Get all application by the status")
    @GetMapping(value = "/status/{status}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("@authorisationService.userCanViewMediaApplications(#requesterId)")
    public ResponseEntity<List<MediaApplication>> getApplicationsByStatus(
        @RequestHeader(REQUESTER_ID) String requesterId,
        @PathVariable MediaApplicationStatus status) {
        return ResponseEntity.ok(mediaApplicationService.getApplicationsByStatus(status));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "Action forbidden")
    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("@authorisationService.userCanViewMediaApplications(#requesterId)")
    public ResponseEntity<MediaApplication> getApplicationById(@RequestHeader(REQUESTER_ID) String requesterId,
                                                               @PathVariable UUID id) {
        return ResponseEntity.ok(mediaApplicationService.getApplicationById(id));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Image with id: {id} is returned")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No image found with id: {id}")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "Action forbidden")
    @GetMapping("/image/{id}")
    @PreAuthorize("@authorisationService.userCanViewMediaApplications(#requesterId)")
    public ResponseEntity<Resource> getImageById(@RequestHeader(REQUESTER_ID) String requesterId,
                                                 @PathVariable String id) {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .body(mediaApplicationService.getImageById(id));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = "400", description = "Validation failed message")
    @Operation(summary = "Create a new application")
    @PostMapping(produces = APPLICATION_JSON_VALUE, consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MediaApplication> createApplication(
        @ModelAttribute("application") @Valid MediaApplication application,
        @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(mediaApplicationService.createApplication(application, file));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @Operation(summary = "Update an existing application")
    @PutMapping("/{id}/{status}")
    public ResponseEntity<MediaApplication> updateApplication(@PathVariable UUID id,
                                                              @PathVariable MediaApplicationStatus status) {
        return ResponseEntity.ok(
            mediaApplicationService.updateApplication(id, status));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = MEDIA_APPLICATION)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "Action forbidden")
    @Operation(summary = "Update a media application, sending an update email alongside")
    @PutMapping(value = "/{id}/{status}/reasons", consumes = APPLICATION_JSON_VALUE)
    @PreAuthorize("@authorisationService.userCanUpdateMediaApplications(#requesterId)")
    public ResponseEntity<MediaApplication> updateApplicationRejection(
        @RequestHeader(REQUESTER_ID) String requesterId,
        @RequestBody Map<String, List<String>> reasons,
        @PathVariable MediaApplicationStatus status, @PathVariable UUID id) {
        return ResponseEntity.ok(mediaApplicationService.updateApplication(id, status, reasons));
    }


    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Application deleted")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = NO_MEDIA_APPLICATION_FOUND_WITH_ID)
    @Operation(summary = "Delete an application")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteApplication(@PathVariable UUID id) {
        mediaApplicationService.deleteApplication(id);
        return ResponseEntity.ok("Application deleted");
    }

    @ApiResponse(responseCode = "204", description = NO_CONTENT_MESSAGE)
    @Operation(summary = "Reports all media applications and deletes approved and rejected applications")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/reporting")
    public ResponseEntity<Void> reportApplications() {
        mediaApplicationService.processApplicationsForReporting();
        return ResponseEntity.noContent().build();
    }
}
