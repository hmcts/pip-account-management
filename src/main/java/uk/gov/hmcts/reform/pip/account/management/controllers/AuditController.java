package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLog;
import uk.gov.hmcts.reform.pip.account.management.model.AuditLogDto;
import uk.gov.hmcts.reform.pip.account.management.service.AuditService;

import javax.validation.Valid;

@RestController
@Tag(name = "Account Management - API for managing audit logs")
@IsAdmin
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    private static final String OK_ERROR_CODE = "200";

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "All audit logs returned as a page.")
    @Operation(summary = "Get all audit logs returned as a page")
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAllAuditLogs(
        @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25") int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(auditService.getAllAuditLogs(pageable));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "Newly created audit log returned.")
    @PostMapping
    public ResponseEntity<AuditLog> createAuditLog(@RequestBody @Valid AuditLogDto auditLogDto) {
        return ResponseEntity.ok(auditService.createAuditLog(auditLogDto.toEntity()));
    }

    @ApiResponse(responseCode = OK_ERROR_CODE, description = "All audit logs that have reached max retention deleted.")
    @DeleteMapping
    public ResponseEntity<String> deleteAuditLogs() {
        return ResponseEntity.ok(auditService.deleteAuditLogs());
    }
}
