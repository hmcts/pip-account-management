package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionListTypeService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing list types for subscription")
@RequestMapping("/subscription")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@Valid
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionListTypeController {
    private static final String OK_CODE = "200";
    private static final String CREATED_CODE = "201";
    private static final String BAD_REQUEST_ERROR_CODE = "404";
    private static final String FORBIDDEN_ERROR_CODE = "403";

    private static final String X_REQUESTER_ID_HEADER = "x-requester-id";

    private final SubscriptionListTypeService subscriptionListTypeService;

    @Autowired
    public SubscriptionListTypeController(SubscriptionListTypeService subscriptionListTypeService) {
        this.subscriptionListTypeService = subscriptionListTypeService;
    }

    @PostMapping("/add-list-types/{userId}")
    @PreAuthorize("@subscriptionAuthorisationService.userCanUpdateSubscriptions(#requesterId, #userId)")
    @Operation(summary = "Endpoint to add list type for existing subscription")
    @ApiResponse(responseCode = CREATED_CODE, description = "Subscription successfully updated for user: {userId}")
    @ApiResponse(responseCode = BAD_REQUEST_ERROR_CODE,
        description = "This request object has an invalid format. Please check again.")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is not authorised to update subscriptions for user with ID {userId}")
    public ResponseEntity<String> addListTypesForSubscription(@RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId,
                                                              @PathVariable UUID userId,
                                                              @RequestBody SubscriptionListType subscriptionListType) {
        subscriptionListTypeService.addListTypesForSubscription(subscriptionListType, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format(
                "Location list Type successfully added for user %s",
                userId
            ));
    }

    @PutMapping("/configure-list-types/{userId}")
    @PreAuthorize("@subscriptionAuthorisationService.userCanUpdateSubscriptions(#requesterId, #userId)")
    @Operation(summary = "Endpoint to update list type for existing subscription")
    @ApiResponse(responseCode = OK_CODE, description = "Subscription successfully updated for user: {userId}")
    @ApiResponse(responseCode = BAD_REQUEST_ERROR_CODE,
        description = "This request object has an invalid format. Please check again.")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is not authorised to update subscriptions for user with ID {userId}")
    public ResponseEntity<String> configureListTypesForSubscription(
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId,
        @PathVariable UUID userId,
        @RequestBody SubscriptionListType subscriptionListType
    ) {
        subscriptionListTypeService.configureListTypesForSubscription(subscriptionListType, userId);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format(
                "Location list Type successfully updated for user %s",
                userId
            ));
    }
}
