package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartyConfigurationService;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartySubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing third-party OAuth configuration")
@RequestMapping("/third-party/configuration")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403",
    description = "User with ID {requesterId} is not authorised to manage third-party OAuth configuration")
public class ThirdPartyConfigurationController {
    private static final String OK_STATUS_CODE = "200";
    private static final String CREATED_STATUS_CODE = "201";
    private static final String BAD_REQUEST_STATUS_CODE = "400";
    private static final String NOT_FOUND_STATUS_CODE = "404";
    private static final String X_REQUESTER_ID_HEADER = "x-requester-id";

    private final ThirdPartyConfigurationService thirdPartyConfigurationService;
    private final ThirdPartySubscriptionNotificationService thirdPartySubscriptionNotificationService;

    @Autowired
    public ThirdPartyConfigurationController(
        ThirdPartyConfigurationService thirdPartyConfigurationService,
        ThirdPartySubscriptionNotificationService thirdPartySubscriptionNotificationService
    ) {
        this.thirdPartyConfigurationService = thirdPartyConfigurationService;
        this.thirdPartySubscriptionNotificationService = thirdPartySubscriptionNotificationService;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Endpoint to create a new third-party OAuth configuration")
    @ApiResponse(responseCode = CREATED_STATUS_CODE,
        description = "Third-party OAuth configuration successfully created for user with ID {userId}")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE,
        description = "The third-party OAuth configuration has an invalid format")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<String> createThirdPartyConfiguration(
        @RequestBody ApiOauthConfiguration apiOauthConfiguration,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        thirdPartyConfigurationService.createThirdPartyConfiguration(apiOauthConfiguration);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Third-party OAuth configuration successfully created for user with ID %s",
                                apiOauthConfiguration.getUserId()));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Endpoint to retrieve a third-party OAuth configuration by user ID")
    @ApiResponse(responseCode = OK_STATUS_CODE,
        description = "Third-party OAuth configuration for user with ID {userId} retrieved")
    @ApiResponse(responseCode = NOT_FOUND_STATUS_CODE,
        description = "Third-party OAuth configuration for user with ID {userId} not found")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<ApiOauthConfiguration> getThirdPartyConfiguration(
        @PathVariable UUID userId,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        return ResponseEntity.ok(thirdPartyConfigurationService.findThirdPartyConfigurationByUserId(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Endpoint to update third-party OAuth configuration by user ID")
    @ApiResponse(responseCode = OK_STATUS_CODE,
        description = "Third-party OAuth configuration successfully updated for user with ID {userId}")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE,
        description = "The third-party OAuth configuration has an invalid format")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    @Transactional
    public ResponseEntity<String> updateThirdPartyConfiguration(
        @PathVariable UUID userId,
        @RequestBody ApiOauthConfiguration apiOauthConfiguration,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        thirdPartyConfigurationService.updateThirdPartyConfigurationByUserId(userId, apiOauthConfiguration);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format("Third-party OAuth configuration successfully updated for user with ID %s", userId));
    }

    @GetMapping
    @Operation(summary = "Endpoint to perform third-party healthcheck using OAuth configuration")
    @ApiResponse(responseCode = OK_STATUS_CODE,
        description = "Successfully performed third-party healthcheck using OAuth configuration")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE,
        description = "The third-party OAuth configuration has an invalid format")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<String> thirdPartyConfigurationHealthCheck(
        @RequestBody ApiOauthConfiguration apiOauthConfiguration,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        thirdPartySubscriptionNotificationService.thirdPartyHealthCheck(apiOauthConfiguration);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format("Successfully performed healthcheck to third-party user with ID %s",
                                apiOauthConfiguration.getUserId()));
    }
}
