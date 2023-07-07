package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLogDto;
import uk.gov.hmcts.reform.pip.account.management.service.AuditService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@Tag(name = "Account Management - API for managing audit logs")
@IsAdmin
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    private static final String OK_ERROR_CODE = "200";
    private static final String AUTH_ERROR_CODE = "403";
    private static final String NOT_FOUND_ERROR_CODE = "404";
    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "All audit logs returned as a page.")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Get all audit logs returned as a page")
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(
        @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25") int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(auditService.getAllAuditLogs(pageable));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Audit log with id {id} returned.")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "Audit log with id {id} could not be found.")
    @Operation(summary = "Get audit log with id")
    @GetMapping(value = "/{id}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<AuditLog> getAuditLogById(@PathVariable UUID id) {
        return ResponseEntity.ok(auditService.getAuditLogById(id));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Newly created audit log returned.")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @PostMapping
    public ResponseEntity<AuditLog> createAuditLog(@RequestBody @Valid AuditLogDto auditLogDto) {
        return ResponseEntity.ok(auditService.createAuditLog(auditLogDto.toEntity()));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "All audit logs that have reached max retention deleted.")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @DeleteMapping
    public ResponseEntity<String> deleteAuditLogs() {
        return ResponseEntity.ok(auditService.deleteAuditLogs());
    }
}
