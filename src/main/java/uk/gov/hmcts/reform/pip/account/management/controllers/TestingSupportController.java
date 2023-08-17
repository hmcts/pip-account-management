package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;
import uk.gov.hmcts.reform.pip.account.management.service.MediaApplicationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management Testing Support API")
@RequestMapping("/testing-support")
@IsAdmin
@Validated
@ConditionalOnProperty(prefix = "testingSupport", name = "enableApi", havingValue = "true")
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestingSupportController {
    private static final String ISSUER_ID = "TESTING-SUPPORT";
    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    private static final String OK_CODE = "200";
    private static final String CREATED_CODE = "201";
    private static final String BAD_REQUEST_CODE = "400";
    private static final String AUTH_ERROR_CODE = "403";

    @Autowired
    private AccountService accountService;

    @Autowired
    private MediaApplicationService mediaApplicationService;

    @ApiResponse(responseCode = CREATED_CODE, description = "{PiUser}")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
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
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Delete all accounts with email prefix")
    @DeleteMapping("/account/{emailPrefix}")
    @Transactional
    public ResponseEntity<String> deleteAccountsWithEmailPrefix(@PathVariable String emailPrefix) {
        return ResponseEntity.ok(accountService.deleteAllAccountsWithEmailPrefix(emailPrefix));
    }

    @ApiResponse(responseCode = OK_CODE,
        description = "Media application(s) deleted with email starting with {emailPrefix}")
    @ApiResponse(responseCode = AUTH_ERROR_CODE, description = NOT_AUTHORIZED_MESSAGE)
    @Operation(summary = "Delete all media applications with email prefix")
    @DeleteMapping("application/{emailPrefix}")
    @Transactional
    public ResponseEntity<String> deleteMediaApplicationsWithEmailPrefix(@PathVariable String emailPrefix) {
        return ResponseEntity.ok(mediaApplicationService.deleteAllApplicationsWithEmailPrefix(emailPrefix));
    }
}
