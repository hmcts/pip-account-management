package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.account.management.service.AuditService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "Account Management - API for managing audit logs")
@RequestMapping("/audit")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public class AuditController {

    private final AuditService auditService;

    private static final String REQUESTER_ID = "x-requester-id";

    private static final String OK_ERROR_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";
    private static final String FORBIDDEN_ERROR_CODE = "401";

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "All audit logs returned as a page with filtering.")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "Action forbidden")
    @Operation(summary = "Get all audit logs returned as a page")
    @GetMapping
    @PreAuthorize("@authorisationService.userCanRequestAuditLogs(#requesterId)")
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(
        @RequestHeader(REQUESTER_ID) String requesterId,
        @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25") int pageSize,
        @RequestParam(name = "email", defaultValue = "", required = false) String email,
        @RequestParam(name = "userId", defaultValue = "", required = false) String userId,
        @RequestParam(name = "actions", defaultValue = "", required = false) List<AuditAction> auditActions,
        @RequestParam(name = "filterDate", defaultValue = "", required = false) String filterDate) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(auditService.getAllAuditLogs(pageable, email, userId,
            auditActions, filterDate));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Audit log with id {id} returned.")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "Audit log with id {id} could not be found.")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "Action forbidden")
    @Operation(summary = "Get audit log with id")
    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    @PreAuthorize("@authorisationService.userCanRequestAuditLogs(#requesterId)")
    public ResponseEntity<AuditLog> getAuditLogById(@RequestHeader(REQUESTER_ID) String requesterId,
                                                    @PathVariable UUID id) {
        return ResponseEntity.ok(auditService.getAuditLogById(id));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Newly created audit log returned.")
    @PostMapping
    public ResponseEntity<AuditLog> createAuditLog(@RequestBody @Valid AuditLog auditLog) {
        return ResponseEntity.ok(auditService.createAuditLog(auditLog));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "All audit logs that have reached max retention deleted.")
    @DeleteMapping
    public ResponseEntity<String> deleteAuditLogs() {
        return ResponseEntity.ok(auditService.deleteAuditLogs());
    }
}
