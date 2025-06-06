package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.service.AuditService;
import uk.gov.hmcts.reform.pip.account.management.service.MediaApplicationService;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionLocationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management Testing Support API")
@RequestMapping("/testing-support")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
@Validated
@AllArgsConstructor
@ConditionalOnProperty(prefix = "testingSupport", name = "enableApi", havingValue = "true")
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestingSupportController {
    private static final String ISSUER_ID = "TESTING-SUPPORT";

    private static final String OK_CODE = "200";
    private static final String CREATED_CODE = "201";
    private static final String BAD_REQUEST_CODE = "400";

    private final AccountService accountService;
    private final MediaApplicationService mediaApplicationService;
    private final SubscriptionLocationService subscriptionLocationService;
    private final AuditService auditService;

    @ApiResponse(responseCode = CREATED_CODE, description = "{PiUser}")
    @ApiResponse(responseCode = BAD_REQUEST_CODE, description = "Failed to create user account")
    @Operation(summary = "Create an account with supplied email and password")
    @PostMapping("/account")
    public ResponseEntity createAccount(@RequestBody AzureAccount azureAccount) {
        Pair<CreationEnum, Object> returnedUser = accountService.addUserWithSuppliedPassword(azureAccount, ISSUER_ID);
        if (returnedUser.getKey() == CreationEnum.CREATED_ACCOUNTS) {
            return ResponseEntity.status(HttpStatus.CREATED)
                .body(returnedUser.getValue());
        }
        return ResponseEntity.badRequest()
            .body(returnedUser.getValue());
    }

    @ApiResponse(responseCode = OK_CODE, description = "Account(s) deleted with email starting with {emailPrefix}")
    @Operation(summary = "Delete all accounts with email prefix")
    @DeleteMapping("/account/{emailPrefix}")
    @Transactional
    public ResponseEntity<String> deleteAccountsWithEmailPrefix(@PathVariable String emailPrefix) {
        return ResponseEntity.ok(accountService.deleteAllAccountsWithEmailPrefix(emailPrefix));
    }

    @ApiResponse(responseCode = OK_CODE,
        description = "Media application(s) deleted with email starting with {emailPrefix}")
    @Operation(summary = "Delete all media applications with email prefix")
    @DeleteMapping("application/{emailPrefix}")
    @Transactional
    public ResponseEntity<String> deleteMediaApplicationsWithEmailPrefix(@PathVariable String emailPrefix) {
        return ResponseEntity.ok(mediaApplicationService.deleteAllApplicationsWithEmailPrefix(emailPrefix));
    }

    @ApiResponse(responseCode = OK_CODE,
        description = "Subscription(s) deleted for location name starting with {locationNamePrefix}")
    @Operation(summary = "Delete all subscriptions with location name prefix")
    @DeleteMapping("/subscription/{locationNamePrefix}")
    @Transactional
    public ResponseEntity<String> deleteSubscriptionsWithLocationNamePrefix(@PathVariable String locationNamePrefix) {
        return ResponseEntity.ok(
            subscriptionLocationService.deleteAllSubscriptionsWithLocationNamePrefix(locationNamePrefix)
        );
    }

    @ApiResponse(responseCode = OK_CODE,
        description = "Audit logs deleted with email starting with {emailPrefix}")
    @Operation(summary = "Delete all audit logs created by user with email prefix")
    @DeleteMapping("audit/{emailPrefix}")
    @Transactional
    public ResponseEntity<String> deleteAuditLogsWithEmailPrefix(@PathVariable String emailPrefix) {
        return ResponseEntity.ok(auditService.deleteAllLogsWithUserEmailPrefix(emailPrefix));
    }

    @ApiResponse(responseCode = OK_CODE,
        description = "Audit log with id {auditId} updated")
    @Operation(summary = "Update the timestamp of audit log with id")
    @PutMapping("audit/{auditId}")
    @Transactional
    public ResponseEntity<String> updateAuditLogTimestampWithId(@PathVariable String auditId) {
        return ResponseEntity.ok(auditService.updateAuditTimestampWithAuditId(auditId));
    }
}
