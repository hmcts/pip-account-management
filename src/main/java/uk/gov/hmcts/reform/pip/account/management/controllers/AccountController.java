package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing accounts")
@RequestMapping("/account")
@Validated
@IsAdmin
public class AccountController {

    @Autowired
    private AccountService accountService;

    private static final String ISSUER_ID = "x-issuer-id";
    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    private static final String AUTH_ERROR_CODE = "401";
    private static final String FORBIDDEN_ERROR_CODE = "403";
    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";

    private static final String PI_USER = "{piUser}";

    /**
     * POST endpoint to create a new user in the P&I postgres database.
     *
     * @param issuerId The id of the user creating the account.
     * @param users       The list of users to add to the database.
     * @return the uuid of the created and added users with any errored accounts
     */
    @ApiResponse(responseCode = "201",
            description = "CREATED_ACCOUNTS: [{Created User UUID's}]")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Add a user to the P&I postgres database")
    @PostMapping("/add/pi")
    public ResponseEntity<Map<CreationEnum, List<?>>> createUsers(//NOSONAR
        @RequestHeader(ISSUER_ID) String issuerId,
        @RequestBody List<PiUser> users) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.addUsers(users, issuerId));
    }

    @ApiResponse(responseCode = OK_CODE, description = PI_USER)
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No user found with the "
            + "user Id: {userId}")
    @Operation(summary = "Get a user based on their user ID")
    @GetMapping("/{userId}")
    public ResponseEntity<PiUser> getUserById(@PathVariable UUID userId) {
        return ResponseEntity.ok(accountService.getUserById(userId));
    }

    @ApiResponse(responseCode = OK_CODE, description = PI_USER)
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No user found with the "
            + "provenance user Id: {provenanceUserId}")
    @Operation(summary = "Get a user based on their provenance user Id and provenance")
    @GetMapping("/provenance/{userProvenance}/{provenanceUserId}")
    public ResponseEntity<PiUser> getUserByProvenanceId(@PathVariable UserProvenances userProvenance,
                                                        @PathVariable String provenanceUserId) {
        return ResponseEntity.ok(accountService.findUserByProvenanceId(userProvenance, provenanceUserId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "User has access to provided publication")
    @ApiResponse(responseCode = AUTH_ERROR_CODE,
            description = "User: {userId} does not have sufficient permission to view list type: {listType}")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No user found with the userId: {userId}")
    @Operation(summary = "Check if a user can see a classified publication through "
        + "their user ID and the publications list type and sensitivity")
    @GetMapping("/isAuthorised/{userId}/{listType}/{sensitivity}")
    public ResponseEntity<Boolean> checkUserAuthorised(@PathVariable UUID userId,
                                                       @PathVariable ListType listType,
                                                       @PathVariable Sensitivity sensitivity) {
        return ResponseEntity.ok(accountService.isUserAuthorisedForPublication(userId, listType, sensitivity));
    }

    @ApiResponse(responseCode = OK_CODE, description = "{Map<String, Optional>}")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Get a map of userId and email from a list of userIds")
    @PostMapping("/emails")
    public ResponseEntity<Map<String, Optional<String>>> getUserEmailsByIds(@RequestBody List<String> userIdsList) {
        return ResponseEntity.ok(accountService.findUserEmailsByIds(userIdsList));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Account with provenance {userProvenance} and provenance id "
            + "{provenanceUserId} has been updated")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "User with supplied provenance "
            + "id: {provenanceUserId} could not be found")
    @Operation(summary = "Update the user's account based on their provenance user id and provenance")
    @PutMapping("/provenance/{userProvenance}/{provenanceUserId}")
    public ResponseEntity<String> updateAccount(@PathVariable UserProvenances userProvenance,
                                                @PathVariable String provenanceUserId,
                                                @RequestBody Map<String, String> params) {
        return ResponseEntity.ok(accountService.updateAccount(userProvenance, provenanceUserId, params));
    }

    @ApiResponse(responseCode = OK_CODE, description = "String confirming deletion")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "User not found")
    @Operation(summary = "Delete a user by their id")
    @DeleteMapping("/delete/{userId}")
    public ResponseEntity<String> deleteAccount(@PathVariable UUID userId) {
        accountService.deleteAccount(userId);
        return ResponseEntity.ok("User deleted");
    }

    @ApiResponse(responseCode = OK_CODE, description = "String confirming update")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "User not found")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE, description = "User with id %s is unable to update user ID %s")
    @Operation(summary = "Update a users role by their id")
    @PutMapping("/update/{userId}/{role}")
    public ResponseEntity<String> updateAccountRoleById(@PathVariable UUID userId,
                                                        @PathVariable Roles role,
                                                        @RequestHeader(value = "x-admin-id", required = false)
                                                            UUID adminUser) {
        return ResponseEntity.ok(accountService.updateAccountRole(adminUser, userId, role));
    }
}
