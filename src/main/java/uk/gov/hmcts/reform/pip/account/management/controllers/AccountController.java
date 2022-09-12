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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;

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
     * @param issuerId The id of the user creating the accounts.
     * @param azureAccounts The accounts to add.
     * @return A list containing details of any created and errored azureAccounts.
     */
    @ApiResponses({
        @ApiResponse(code = 200, message = "{AzureAccount}"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
    })
    @PostMapping("/add/azure")
    public ResponseEntity<Map<CreationEnum, List<? extends AzureAccount>>> createAzureAccount(
        @RequestHeader("x-issuer-id") String issuerId,
        @RequestBody List<AzureAccount> azureAccounts) {
        return ResponseEntity.ok(accountService.addAzureAccounts(azureAccounts, issuerId, false));
    }

    /**
     * POST endpoint to create a new user in the P&I postgres database.
     *
     * @param issuerId The id of the user creating the account.
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
        @RequestHeader("x-issuer-id") String issuerId,
        @RequestBody List<PiUser> users) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.addUsers(users, issuerId));
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
    @ApiOperation("Check if a user can see a classified publication through "
        + "their user ID and the publications list type and sensitivity")
    @GetMapping("/isAuthorised/{userId}/{listType}/{sensitivity}")
    public ResponseEntity<Boolean> checkUserAuthorised(@PathVariable UUID userId,
                                                       @PathVariable ListType listType,
                                                       @PathVariable Sensitivity sensitivity) {
        return ResponseEntity.ok(accountService.isUserAuthorisedForPublication(userId, listType, sensitivity));
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

    @ApiResponses({
        @ApiResponse(code = 200,
            message = "CREATED_ACCOUNTS:[{Created user ids}], ERRORED_ACCOUNTS: [{failed accounts}]"),
        @ApiResponse(code = 400, message = "Bad request"),
    })
    @ApiOperation("Create media accounts via CSV upload")
    @PostMapping("/media-bulk-upload")
    public ResponseEntity<Map<CreationEnum, List<?>>> createMediaAccountsBulk(
        @RequestHeader("x-issuer-id") String issuerId, @RequestPart MultipartFile mediaList) {
        return ResponseEntity.ok(accountService.uploadMediaFromCsv(mediaList, issuerId));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "Account with provenance id {provenanceUserId} has been verified"),
        @ApiResponse(code = 403, message = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(code = 404, message = "User with supplied provenance id: {provenanceUserId} could not be found"),
    })
    @ApiOperation("Update the last verified date for an account")
    @PutMapping("/verification/{provenanceUserId}")
    public ResponseEntity<String> updateAccountVerification(@PathVariable String provenanceUserId) {
        return ResponseEntity.ok(accountService.updateAccountVerification(provenanceUserId));
    }
}
