package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.service.BulkAccountCreationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Account Management - API for bulk create media accounts")
@RequestMapping("/account")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@Validated
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class BulkAccountCreationController {
    private static final String ISSUER_ID = "x-issuer-id";
    private static final String OK_CODE = "200";
    private static final String ERROR_CODE = "400";
    private static final String FORBIDDEN_ERROR_CODE = "401";

    private final BulkAccountCreationService bulkAccountCreationService;

    @ApiResponse(responseCode = OK_CODE,
        description = "CREATED_ACCOUNTS:[{Created user ids}], ERRORED_ACCOUNTS: [{failed accounts}]")
    @ApiResponse(responseCode = ERROR_CODE, description = "Bad request")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "Action forbidden")
    @Operation(summary = "Create media accounts via CSV upload")
    @PreAuthorize("@authorisationService.userCanBulkCreateMediaAccounts(#issuerId)")
    @PostMapping("/media-bulk-upload")
    public ResponseEntity<Map<CreationEnum, List<?>>> createMediaAccountsBulk(//NOSONAR
        @RequestHeader(ISSUER_ID) String issuerId, @RequestPart MultipartFile mediaList) {
        return ResponseEntity.ok(bulkAccountCreationService.uploadMediaFromCsv(mediaList, issuerId));
    }
}
