package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountFilteringService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;

import java.util.List;

@RestController
@Tag(name = "Account Management - API for retrieving custom user accounts")
@RequestMapping("/account")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@Validated
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
@SuppressWarnings("squid:S1133")
public class AccountFilteringController {
    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";

    private static final String PI_USER = "{piUser}";

    private final AccountFilteringService accountFilteringService;

    @Autowired
    public AccountFilteringController(AccountFilteringService accountFilteringService) {
        this.accountFilteringService = accountFilteringService;
    }

    @ApiResponse(responseCode = OK_CODE, description = "List of Accounts MI Data")
    @Operation(summary = "Returns anonymized account data for MI reporting")
    @GetMapping("/mi-data")
    public ResponseEntity<List<AccountMiData>> getMiData() {
        return ResponseEntity.status(HttpStatus.OK).body(accountFilteringService.getAccountDataForMi());
    }

    @ApiResponse(responseCode = OK_CODE, description = "List of third party accounts")
    @Operation(summary = "Get all third party accounts")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/all/third-party")
    public ResponseEntity<List<PiUser>> getAllThirdPartyAccounts() {
        return ResponseEntity.ok(accountFilteringService.findAllThirdPartyAccounts());
    }

    @Operation(summary = "Get all accounts except third party in a page with filtering")
    @GetMapping("/all")
    public ResponseEntity<Page<PiUser>> getAllAccountsExceptThirdParty(
        @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
        @RequestParam(name = "pageSize", defaultValue = "25") int pageSize,
        @RequestParam(name = "email", defaultValue = "", required = false) String email,
        @RequestParam(name = "userProvenanceId", defaultValue = "", required = false) String userProvenanceId,
        @RequestParam(name = "provenances", defaultValue = "", required = false) List<UserProvenances> provenances,
        @RequestParam(name = "roles", defaultValue = "", required = false) List<Roles> roles,
        @RequestParam(name = "userId", defaultValue = "", required = false) String userId) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        return ResponseEntity.ok(accountFilteringService.findAllAccountsExceptThirdParty(
            pageable, email, userProvenanceId, provenances, roles, userId
        ));
    }

    @ApiResponse(responseCode = OK_CODE, description = PI_USER)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No user found with the "
        + "email: {email} and provenance {provenance}")
    @Operation(summary = "Get an Admin user (excluding system admin) based on their email and provenance")
    @GetMapping("/admin/{email}/{provenance}")
    public ResponseEntity<PiUser> getAdminUserByEmailAndProvenance(@PathVariable String email,
                                                                   @PathVariable UserProvenances provenance) {
        return ResponseEntity.ok(accountFilteringService.getAdminUserByEmailAndProvenance(email, provenance));
    }
}
