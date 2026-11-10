package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;
import uk.gov.hmcts.reform.pip.account.management.service.UserCommunicationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;

@RestController
@Tag(name = "Account Management - API for managing user communications")
@RequestMapping("/communication")
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserCommunicationController {
    private static final String OK_STATUS_CODE = "200";

    private final UserCommunicationService userCommunicationService;

    @ApiResponse(responseCode = OK_STATUS_CODE, description = "All user communications retrieved successfully")
    @Operation(summary = "Get all current user communications")
    @GetMapping()
    public ResponseEntity<List<UserCommunication>> getUserCommunications() {
        return ResponseEntity.ok(userCommunicationService.getAll());
    }
}
