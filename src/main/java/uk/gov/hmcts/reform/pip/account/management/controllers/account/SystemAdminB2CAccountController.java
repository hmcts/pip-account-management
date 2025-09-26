package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.service.account.SystemAdminB2CAccountService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing B2C system admin accounts")
@RequestMapping("/account")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@Validated
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SystemAdminB2CAccountController {
    private static final String REQUESTER_ID = "x-requester-id";
    private static final String OK_CODE = "200";
    private static final String BAD_REQUEST_CODE = "400";
    private static final String FORBIDDEN_ERROR_CODE = "403";
    private static final String PI_USER = "{piUser}";

    private final SystemAdminB2CAccountService systemAdminB2CAccountService;

    /**
     * POST endpoint that deals with creating a new System Admin Account (including PI and Azure)
     * This will also trigger any welcome emails.
     *
     * @param requesterId The id of the user creating the accounts.
     * @param account The account to add.
     * @return The PiUser that's been added, or an ErroredPiUser if it failed to add.
     */
    @ApiResponse(responseCode = OK_CODE, description = PI_USER)
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "{ErroredSystemAdminAccount}")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is forbidden to create a B2C system admin")
    @PostMapping("/add/system-admin")
    @PreAuthorize("@accountAuthorisationService.userCanCreateSystemAdmin(#requesterId)")
    public ResponseEntity<? extends PiUser> createSystemAdminAccount(//NOSONAR
        @RequestHeader(REQUESTER_ID) UUID requesterId, @RequestBody SystemAdminAccount account) {
        return ResponseEntity.ok(systemAdminB2CAccountService.addSystemAdminAccount(account, requesterId));
    }
}
