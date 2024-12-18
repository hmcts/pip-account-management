package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.service.SystemAdminB2CAccountService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management - API for managing B2C system admin accounts")
@RequestMapping("/account")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@Validated
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SystemAdminB2CAccountController {
    private static final String ISSUER_ID = "x-issuer-id";
    private static final String OK_CODE = "200";
    private static final String BAD_REQUEST_CODE = "400";
    private static final String PI_USER = "{piUser}";

    private final SystemAdminB2CAccountService systemAdminB2CAccountService;

    /**
     * POST endpoint that deals with creating a new System Admin Account (including PI and Azure)
     * This will also trigger any welcome emails.
     *
     * @param issuerId The id of the user creating the accounts.
     * @param account The account to add.
     * @return The PiUser that's been added, or an ErroredPiUser if it failed to add.
     */
    @ApiResponse(responseCode = OK_CODE, description = PI_USER)
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "{ErroredSystemAdminAccount}")
    @PostMapping("/add/system-admin")
    public ResponseEntity<? extends PiUser> createSystemAdminAccount(//NOSONAR
        @RequestHeader(ISSUER_ID) String issuerId, @RequestBody SystemAdminAccount account) {
        return ResponseEntity.ok(systemAdminB2CAccountService.addSystemAdminAccount(account, issuerId));
    }
}
