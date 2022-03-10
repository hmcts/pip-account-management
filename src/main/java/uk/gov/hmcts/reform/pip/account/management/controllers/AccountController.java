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
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidEmail;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@Api(tags = "Account Management - API for managing accounts")
@RequestMapping("/account")
@Validated
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * POST endpoint to create a new subscriber account.
     * This will also trigger any welcome emails.
     *
     * @param subscribers The subscribers to add.
     * @return The ID for the subscriber.
     */
    @PostMapping("/add")
    public ResponseEntity<Map<CreationEnum, List<? extends Subscriber>>> createSubscriber(
        @RequestBody List<Subscriber> subscribers) {
        Map<CreationEnum, List<? extends Subscriber>> processedSubscribers =
            accountService.createSubscribers(subscribers);
        return ResponseEntity.ok(processedSubscribers);
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
            message = "CREATED_ACCOUNTS: [{Created User UUID's}]")
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
    public ResponseEntity<Boolean> isUserAuthorised(@PathVariable UUID userId, @PathVariable ListType listType) {
        return ResponseEntity.ok(accountService.isUserAuthorisedForPublication(userId, listType));
    }
}
