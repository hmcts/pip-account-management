package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.account.UserWithSubscriptions;
import uk.gov.hmcts.reform.pip.account.management.service.MigrationSupportService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.time.LocalDate;
import java.util.List;

@RestController
@Tag(name = "Account Management Migration Support API")
@RequestMapping("/migration-support")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
@Validated
@AllArgsConstructor
@ConditionalOnProperty(prefix = "migrationSupport", name = "enableApi", havingValue = "true")
public class MigrationSupportController {

    private static final String OK_CODE = "200";
    private static final String BAD_REQUEST_CODE = "400";

    private final MigrationSupportService migrationSupportService;

    @ApiResponse(responseCode = OK_CODE, description = "List of users with their subscriptions")
    @ApiResponse(responseCode = BAD_REQUEST_CODE,
        description = "Invalid date supplied, or lastSignedInDateFrom is after lastSignedInDateTo")
    @Operation(summary = "Get all users with their subscriptions, optionally filtered by last signed in date "
        + "(inclusive). Users without a last signed in date are filtered on their created date.")
    @GetMapping("/account")
    public ResponseEntity<List<UserWithSubscriptions>> getAllUsersWithSubscriptions(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastSignedInDateFrom,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastSignedInDateTo) {
        return ResponseEntity.ok(
            migrationSupportService.getAllUsersWithSubscriptions(lastSignedInDateFrom, lastSignedInDateTo)
        );
    }
}
