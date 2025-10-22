package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.service.account.AzureAccountService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Tag(name = "Account Management - API for managing Azure accounts")
@RequestMapping("/account")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@Validated
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class AzureAccountController {
    private static final String REQUESTER_ID = "x-requester-id";

    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";
    private static final String FORBIDDEN_ERROR_CODE = "403";

    private final AzureAccountService azureAccountService;

    /**
     * POST endpoint to create a new azure account.
     * This will also trigger any welcome emails.
     *
     * @param requesterId The id of the user creating the accounts.
     * @param azureAccounts The accounts to add.
     * @return A list containing details of any created and errored azureAccounts.
     */
    @ApiResponse(responseCode = OK_CODE, description = "{AzureAccount}")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID %s {requesterId} forbidden to create user")
    @PreAuthorize("@accountAuthorisationService.userCanCreateAzureAccount(#requesterId)")
    @PostMapping("/add/azure")
    public ResponseEntity<Map<CreationEnum, List<? extends AzureAccount>>> createAzureAccount(//NOSONAR
        @RequestHeader(REQUESTER_ID) UUID requesterId, @RequestBody List<AzureAccount> azureAccounts) {
        return ResponseEntity.ok(azureAccountService.addAzureAccounts(azureAccounts, requesterId, false, false));
    }

    @ApiResponse(responseCode = OK_CODE, description = "P&I Azure User Information")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "User not found")
    @Operation(summary = "Get P&I Azure User info")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/azure/{provenanceUserId}")
    public ResponseEntity<AzureAccount> getAzureAccountInfo(@PathVariable String provenanceUserId) {
        return ResponseEntity.ok(azureAccountService.retrieveAzureAccount(provenanceUserId));
    }

}
