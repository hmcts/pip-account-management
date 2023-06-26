package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;
import uk.gov.hmcts.reform.pip.account.management.service.MediaApplicationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management Testing Support API")
@RequestMapping("/testing-support")
@IsAdmin
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestingSupportController {
    private static final String NOT_AUTHORIZED_MESSAGE = "User has not been authorized";

    private static final String OK_CODE = "200";
    private static final String AUTH_ERROR_CODE = "403";

    @Autowired
    private AccountService accountService;

    @Autowired
    private MediaApplicationService mediaApplicationService;

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
