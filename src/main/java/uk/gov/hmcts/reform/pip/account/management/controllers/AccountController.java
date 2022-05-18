package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidEmail;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@Api(tags = "Account Management - API for managing accounts")
@RequestMapping("/account")
@Validated
@IsAdmin
public class AccountController {

    @Autowired
    private AccountService accountService;

    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    /**
     * POST endpoint to create a new azure account.
     * This will also trigger any welcome emails.
     *
     * @param issuerEmail The user creating the accounts.
     * @param azureAccounts The accounts to add.
     * @return A list containing details of any created and errored azureAccounts.
     */
    @ApiResponses({
        @ApiResponse(code = 200, message = "{AzureAccount}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
    })
    @PostMapping("/add/azure")
    public ResponseEntity<Map<CreationEnum, List<? extends AzureAccount>>> createAzureAccount(
        @RequestHeader("x-issuer-email") @ValidEmail String issuerEmail,
        @RequestBody List<AzureAccount> azureAccounts) {
        return ResponseEntity.ok(accountService.addAzureAccounts(azureAccounts, issuerEmail));
    }

    /**
     * POST endpoint to create a new user in the P&I postgres database.
     *
     * @param issuerEmail The user creating the account.
     * @param users       The list of users to add to the database.
     * @return the uuid of the created and added users with any errored accounts
     */
    @ApiResponses({
        @ApiResponse(code = 201,
            message = "CREATED_ACCOUNTS: [{Created User UUID's}]"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
    })
    @ApiOperation("Add a user to the P&I postgres database")
    @PostMapping("/add/pi")
    public ResponseEntity<Map<CreationEnum, List<?>>> createUsers(
        @RequestHeader("x-issuer-email") @ValidEmail String issuerEmail,
        @RequestBody List<PiUser> users) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.addUsers(users, issuerEmail));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "{PiUser}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(code = 404, message = "No user found with the provenance user Id: {provenanceUserId}")
    })
    @ApiOperation("Get a user based on their provenance user Id and provenance")
    @GetMapping("/provenance/{userProvenance}/{provenanceUserId}")
    public ResponseEntity<PiUser> getUserByProvenanceId(@PathVariable UserProvenances userProvenance,
                                                        @PathVariable String provenanceUserId) {
        return ResponseEntity.ok(accountService.findUserByProvenanceId(userProvenance, provenanceUserId));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "User has access to provided publication"),
        @ApiResponse(code = 403,
            message = "User: {userId} does not have sufficient permission to view list type: {listType}"),
        @ApiResponse(code = 404, message = "No user found with the userId: {userId}"),
    })
    @ApiOperation("Check if a user can see a classified publication through list type and their provenance")
    @GetMapping("/isAuthorised/{userId}/{listType}")
    public ResponseEntity<Boolean> checkUserAuthorised(@PathVariable UUID userId, @PathVariable ListType listType) {
        return ResponseEntity.ok(accountService.isUserAuthorisedForPublication(userId, listType));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "{Map<String, Optional>}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE)
    })
    @ApiOperation("Get a map of userId and email from a list of userIds")
    @PostMapping("/emails")
    public ResponseEntity<Map<String, Optional<String>>> getUserEmailsByIds(@RequestBody List<String> userIdsList) {
        return ResponseEntity.ok(accountService.findUserEmailsByIds(userIdsList));
    }
}
