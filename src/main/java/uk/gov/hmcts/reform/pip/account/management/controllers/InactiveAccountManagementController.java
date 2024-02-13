package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.service.InactiveAccountManagementService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management - API for managing inactive user accounts")
@RequestMapping("/account")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@Validated
@IsAdmin
@SecurityRequirement(name = "Bearer authentication")
public class InactiveAccountManagementController {
    private static final String NO_CONTENT_MESSAGE = "The request has been successfully fulfilled";
    private static final String NO_CONTENT_CODE = "204";

    private final InactiveAccountManagementService inactiveAccountManagementService;

    @Autowired
    public InactiveAccountManagementController(InactiveAccountManagementService inactiveAccountManagementService) {
        this.inactiveAccountManagementService = inactiveAccountManagementService;
    }

    @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE)
    @Operation(summary = "Notify inactive media users to verify their accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/media/inactive/notify")
    public ResponseEntity<Void> notifyInactiveMediaAccounts() {
        inactiveAccountManagementService.sendMediaUsersForVerification();
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE)
    @Operation(summary = "Delete all expired inactive accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/media/inactive")
    public ResponseEntity<Void> deleteExpiredMediaAccounts() {
        inactiveAccountManagementService.findMediaAccountsForDeletion();
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE)
    @Operation(summary = "Notify inactive admin users to verify their accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/admin/inactive/notify")
    public ResponseEntity<Void> notifyInactiveAdminAccounts() {
        inactiveAccountManagementService.notifyAdminUsersToSignIn();
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE)
    @Operation(summary = "Delete all expired inactive admin accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/admin/inactive")
    public ResponseEntity<Void> deleteExpiredAdminAccounts() {
        inactiveAccountManagementService.findAdminAccountsForDeletion();
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE)
    @Operation(summary = "Notify inactive idam users to verify their accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/idam/inactive/notify")
    public ResponseEntity<Void> notifyInactiveIdamAccounts() {
        inactiveAccountManagementService.notifyIdamUsersToSignIn();
        return ResponseEntity.noContent().build();
    }

    @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE)
    @Operation(summary = "Delete all expired inactive idam accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/idam/inactive")
    public ResponseEntity<Void> deleteExpiredIdamAccounts() {
        inactiveAccountManagementService.findIdamAccountsForDeletion();
        return ResponseEntity.noContent().build();
    }
}
