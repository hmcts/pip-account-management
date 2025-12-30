package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartySubscriptionService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing third-party user subscriptions")
@RequestMapping("/third-party/subscription")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403",
    description = "User with ID {requesterId} is not authorised to manage third-party user subscriptions")
public class ThirdPartySubscriptionController {
    private static final String OK_STATUS_CODE = "200";
    private static final String CREATED_STATUS_CODE = "201";
    private static final String BAD_REQUEST_STATUS_CODE = "400";
    private static final String NOT_FOUND_STATUS_CODE = "404";
    private static final String X_REQUESTER_ID_HEADER = "x-requester-id";

    private final ThirdPartySubscriptionService thirdPartySubscriptionService;

    @Autowired
    public ThirdPartySubscriptionController(ThirdPartySubscriptionService thirdPartySubscriptionService) {
        this.thirdPartySubscriptionService = thirdPartySubscriptionService;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Endpoint to create new third-party subscriptions")
    @ApiResponse(responseCode = CREATED_STATUS_CODE,
        description = "Third-party subscriptions successfully created for user with ID {userId}")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE,
        description = "A third-party subscription has an invalid format")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<String> createThirdPartySubscriptions(
        @RequestBody List<ApiSubscription> apiSubscriptions,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        if (apiSubscriptions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("The list of third-party subscriptions cannot be empty");
        }
        thirdPartySubscriptionService.createThirdPartySubscriptions(apiSubscriptions);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Third-party subscriptions successfully created for user with ID %s",
                                apiSubscriptions.get(0).getUserId()));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Endpoint to retrieve third-party subscriptions by user ID")
    @ApiResponse(responseCode = OK_STATUS_CODE,
        description = "Third-party subscriptions for user with ID {userId} retrieved")
    @ApiResponse(responseCode = NOT_FOUND_STATUS_CODE,
        description = "Third-party subscriptions for user with ID {userId} not found")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<List<ApiSubscription>> getThirdPartySubscriptions(
        @PathVariable UUID userId,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        return ResponseEntity.ok(thirdPartySubscriptionService.findThirdPartySubscriptionsByUserId(userId));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Endpoint to update third-party subscriptions by user ID")
    @ApiResponse(responseCode = OK_STATUS_CODE,
        description = "Third-party subscriptions successfully updated for user with ID {userId}")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE,
        description = "A third-party subscription has an invalid format")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<String> updateThirdPartySubscriptions(
        @PathVariable UUID userId,
        @RequestBody List<ApiSubscription> apiSubscriptions,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        thirdPartySubscriptionService.updateThirdPartySubscriptionsByUserId(userId, apiSubscriptions);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format("Third-party subscriptions successfully updated for user with ID %s", userId));
    }
}
