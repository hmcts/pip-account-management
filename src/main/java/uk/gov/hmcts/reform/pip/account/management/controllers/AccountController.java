package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
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
import uk.gov.hmcts.reform.pip.account.management.service.AccountVerificationService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing accounts")
@RequestMapping("/account")
@Validated
@IsAdmin
@SuppressWarnings("PMD.TooManyMethods")
public class AccountController {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountVerificationService accountVerificationService;

    private static final String NO_CONTENT_MESSAGE = "The request has been successfully fulfilled";
    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    private static final String AUTH_ERROR_CODE = "403";
    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";
    private static final String NO_CONTENT_CODE = "204";

    /**
     * POST endpoint to create a new azure account.
     * This will also trigger any welcome emails.
     *
     * @param issuerId The id of the user creating the accounts.
     * @param azureAccounts The accounts to add.
     * @return A list containing details of any created and errored azureAccounts.
     */
    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "{AzureAccount}"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE),
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
        @ApiResponse(responseCode = "201",
            description = "CREATED_ACCOUNTS: [{Created User UUID's}]"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE),
    })
    @Operation(summary = "Add a user to the P&I postgres database")
    @PostMapping("/add/pi")
    public ResponseEntity<Map<CreationEnum, List<?>>> createUsers(
        @RequestHeader("x-issuer-id") String issuerId,
        @RequestBody List<PiUser> users) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.addUsers(users, issuerId));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "{PiUser}"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No user found with the "
            + "user Id: {userId}")
    })
    @Operation(summary = "Get a user based on their user ID")
    @GetMapping("/{userId}")
    public ResponseEntity<PiUser> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.getUserById(userId));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "{PiUser}"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No user found with the "
            + "provenance user Id: {provenanceUserId}")
    })
    @Operation(summary = "Get a user based on their provenance user Id and provenance")
    @GetMapping("/provenance/{userProvenance}/{provenanceUserId}")
    public ResponseEntity<PiUser> getUserByProvenanceId(@PathVariable UserProvenances userProvenance,
                                                        @PathVariable String provenanceUserId) {
        return ResponseEntity.ok(accountService.findUserByProvenanceId(userProvenance, provenanceUserId));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "User has access to provided publication"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE,
            description = "User: {userId} does not have sufficient permission to view list type: {listType}"),
        @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No user found with the userId: {userId}"),
    })
    @Operation(summary = "Check if a user can see a classified publication through "
        + "their user ID and the publications list type and sensitivity")
    @GetMapping("/isAuthorised/{userId}/{listType}/{sensitivity}")
    public ResponseEntity<Boolean> checkUserAuthorised(@PathVariable UUID userId,
                                                       @PathVariable ListType listType,
                                                       @PathVariable Sensitivity sensitivity) {
        return ResponseEntity.ok(accountService.isUserAuthorisedForPublication(userId, listType, sensitivity));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "{Map<String, Optional>}"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    })
    @Operation(summary = "Get a map of userId and email from a list of userIds")
    @PostMapping("/emails")
    public ResponseEntity<Map<String, Optional<String>>> getUserEmailsByIds(@RequestBody List<String> userIdsList) {
        return ResponseEntity.ok(accountService.findUserEmailsByIds(userIdsList));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE,
            description = "CREATED_ACCOUNTS:[{Created user ids}], ERRORED_ACCOUNTS: [{failed accounts}]"),
        @ApiResponse(responseCode = "400", description = "Bad request"),
    })
    @Operation(summary = "Create media accounts via CSV upload")
    @PostMapping("/media-bulk-upload")
    public ResponseEntity<Map<CreationEnum, List<?>>> createMediaAccountsBulk(
        @RequestHeader("x-issuer-id") String issuerId, @RequestPart MultipartFile mediaList) {
        return ResponseEntity.ok(accountService.uploadMediaFromCsv(mediaList, issuerId));
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "Account Management - MI Data request accepted.")
    })
    @Operation(summary = "Returns a list of (anonymized) account data for MI reporting.")
    @GetMapping("/mi-data")
    public ResponseEntity<String> getMiData() {
        return ResponseEntity.status(HttpStatus.OK).body(accountService.getAccManDataForMiReporting());
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "Account with provenance {userProvenance} and provenance id "
            + "{provenanceUserId} has been updated"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE),
        @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "User with supplied provenance "
            + "id: {provenanceUserId} could not be found"),
    })
    @Operation(summary = "Update the user's account based on their provenance user id and provenance")
    @PutMapping("/provenance/{userProvenance}/{provenanceUserId}")
    public ResponseEntity<String> updateAccount(@PathVariable UserProvenances userProvenance,
                                                @PathVariable String provenanceUserId,
                                                @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(accountService.updateAccount(userProvenance, provenanceUserId, params));
    }

    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NO_CONTENT_MESSAGE)
    })
    @Operation(summary = "Notify inactive media users to verify their accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/media/inactive/notify")
    public ResponseEntity<Void> notifyInactiveMediaAccounts() {
        accountVerificationService.sendMediaUsersForVerification();
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    })
    @Operation(summary = "Delete all expired inactive accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/media/inactive")
    public ResponseEntity<Void> deleteExpiredMediaAccounts() {
        accountVerificationService.findMediaAccountsForDeletion();
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NO_CONTENT_MESSAGE)
    })
    @Operation(summary = "Notify inactive admin users to verify their accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/admin/inactive/notify")
    public ResponseEntity<Void> notifyInactiveAdminAccounts() {
        accountVerificationService.notifyAdminUsersToSignIn();
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    })
    @Operation(summary = "Delete all expired inactive admin accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/admin/inactive")
    public ResponseEntity<Void> deleteExpiredAdminAccounts() {
        accountVerificationService.findAdminAccountsForDeletion();
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NO_CONTENT_MESSAGE)
    })
    @Operation(summary = "Notify inactive idam users to verify their accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/idam/inactive/notify")
    public ResponseEntity<Void> notifyInactiveIdamAccounts() {
        accountVerificationService.notifyIdamUsersToSignIn();
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(responseCode = NO_CONTENT_CODE, description = NO_CONTENT_MESSAGE),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    })
    @Operation(summary = "Delete all expired inactive idam accounts")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/idam/inactive")
    public ResponseEntity<Void> deleteExpiredIdamAccounts() {
        accountVerificationService.findIdamAccountsForDeletion();
        return ResponseEntity.noContent().build();
    }

    @ApiResponses({
        @ApiResponse(responseCode = OK_CODE, description = "List of thord party accounts"),
        @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    })
    @Operation(summary = "Get all third party accounts")
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/all/third-party")
    public ResponseEntity<List<PiUser>> getAccountsByThirdPartyRole() {
        return ResponseEntity.ok(accountService.findAllThirdPartyAccounts());
    }
}
