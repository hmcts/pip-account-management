package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.service.account.SystemAdminAccountService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management - API for managing system admin accounts")
@RequestMapping("/account")
@Validated
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class SystemAdminAccountController {
    private static final String PI_USER = "{piUser}";

    private final SystemAdminAccountService systemAdminAccountService;

    /**
     * Create a system admin account for SSO user on the user table.
     *
     * @param account The account to add.
     * @return The PiUser that's been added, or an ErroredPiUser if it failed to add.
     */
    @ApiResponse(responseCode = "200", description = PI_USER)
    @ApiResponse(responseCode = "400", description = "{ErroredSystemAdminAccount}")
    @ApiResponse(responseCode = "401", description = "Invalid access credential")
    @ApiResponse(responseCode = "403", description = "User has not been authorized")
    @PostMapping("/system-admin")
    public ResponseEntity<? extends PiUser> createSystemAdminAccount(//NOSONAR
        @RequestBody SystemAdminAccount account) {
        return ResponseEntity.ok(systemAdminAccountService.addSystemAdminAccount(account));
    }
}
