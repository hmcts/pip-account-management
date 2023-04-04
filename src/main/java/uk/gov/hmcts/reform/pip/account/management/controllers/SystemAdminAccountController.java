package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.service.SystemAdminAccountService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management - API for managing system admin accounts")
@RequestMapping("/account")
@Validated
@IsAdmin
public class SystemAdminAccountController {
    private static final String ISSUER_ID = "x-issuer-id";
    private static final String AUTH_ERROR_CODE = "403";
    private static final String OK_CODE = "200";
    private static final String BAD_REQUEST_CODE = "400";
    private static final String PI_USER = "{piUser}";
    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    @Autowired
    private SystemAdminAccountService systemAdminAccountService;

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
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @PostMapping("/add/system-admin")
    public ResponseEntity<? extends PiUser> createSystemAdminAccount(//NOSONAR
        @RequestHeader(ISSUER_ID) String issuerId, @RequestBody SystemAdminAccount account) {
        return ResponseEntity.ok(systemAdminAccountService.addSystemAdminAccount(account, issuerId));
    }
}
