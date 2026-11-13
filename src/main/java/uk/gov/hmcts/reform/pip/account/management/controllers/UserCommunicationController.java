package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;
import uk.gov.hmcts.reform.pip.account.management.service.UserCommunicationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing user communications")
@RequestMapping("/communication")
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
public class UserCommunicationController {
    private static final String REQUESTER_ID_HEADER = "x-requester-id";

    private static final String OK_STATUS_CODE = "200";
    private static final String CREATED_STATUS_CODE = "201";
    private static final String NO_CONTENT_STATUS_CODE = "204";
    private static final String BAD_REQUEST_STATUS_CODE = "400";
    private static final String NOT_FOUND_STATUS_CODE = "404";
    private static final String FORBIDDEN_STATUS_CODE = "403";

    private final UserCommunicationService userCommunicationService;

    @ApiResponse(responseCode = OK_STATUS_CODE, description = "All user communications retrieved successfully")
    @Operation(summary = "Get all current user communications")
    @GetMapping()
    public ResponseEntity<List<UserCommunication>> getUserCommunications() {
        return ResponseEntity.ok(userCommunicationService.getAll());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Endpoint to retrieve an user communication by ID")
    @ApiResponse(responseCode = OK_STATUS_CODE, description = "User communication with ID {id} successfully retrieved")
    @ApiResponse(responseCode = NOT_FOUND_STATUS_CODE, description = "User communication with ID {id} not found")
    @ApiResponse(responseCode = FORBIDDEN_STATUS_CODE,
        description = "User with ID {requesterId} is not authorised to retrieve user communication")
    @PreAuthorize("@userCommunicationAuthorisationService.userCanManageUserCommunication(#requesterId)")
    @Transactional
    public ResponseEntity<UserCommunication> getUserCommunicationById(
        @PathVariable UUID id,
        @RequestHeader(REQUESTER_ID_HEADER) UUID requesterId
    ) {
        return ResponseEntity.status(HttpStatus.OK)
            .body(userCommunicationService.findUserCommunicationById(id));
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Endpoint to create a new user communication")
    @ApiResponse(responseCode = CREATED_STATUS_CODE,
        description = "User communication successfully created for {audienceType} audience type")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE,
        description = "Supplied user communication has invalid format")
    @ApiResponse(responseCode = FORBIDDEN_STATUS_CODE,
        description = "User with ID {requesterId} is not authorised to create user communication")
    @PreAuthorize("@userCommunicationAuthorisationService.userCanManageUserCommunication(#requesterId)")
    public ResponseEntity<String> createUserCommunication(
        @RequestBody UserCommunication userCommunication,
        @RequestHeader(REQUESTER_ID_HEADER) UUID requesterId
    ) {
        userCommunicationService.createUserCommunication(userCommunication);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("User communication successfully created for %s audience type",
                                userCommunication.getAudienceType()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Endpoint to update an existing user communication")
    @ApiResponse(responseCode = OK_STATUS_CODE, description = "User communication with ID {id} successfully updated")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE,
        description = "Supplied user communication has invalid format")
    @ApiResponse(responseCode = NOT_FOUND_STATUS_CODE, description = "User communication with ID {id} not found")
    @ApiResponse(responseCode = FORBIDDEN_STATUS_CODE,
        description = "User with ID {requesterId} is not authorised to update user communication")
    @PreAuthorize("@userCommunicationAuthorisationService.userCanManageUserCommunication(#requesterId)")
    @Transactional
    public ResponseEntity<String> updateUserCommunication(
        @PathVariable UUID id,
        @RequestBody UserCommunication userCommunication,
        @RequestHeader(REQUESTER_ID_HEADER) UUID requesterId
    ) {
        userCommunicationService.updateUserCommunication(userCommunication, id);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format("User communication with ID %s successfully updated", id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Endpoint to delete a user communication")
    @ApiResponse(responseCode = NO_CONTENT_STATUS_CODE,
        description = "User communication with ID {id} successfully deleted")
    @ApiResponse(responseCode = NOT_FOUND_STATUS_CODE, description = "User communication with ID {id} not found")
    @ApiResponse(responseCode = FORBIDDEN_STATUS_CODE,
        description = "User with ID {requesterId} is not authorised to delete user communication")
    @PreAuthorize("@userCommunicationAuthorisationService.userCanManageUserCommunication(#requesterId)")
    @Transactional
    public ResponseEntity<String> deleteUserCommunication(
        @PathVariable UUID id,
        @RequestHeader(REQUESTER_ID_HEADER) UUID requesterId
    ) {
        userCommunicationService.deleteUserCommunication(id);
        return ResponseEntity.noContent().build();
    }
}
