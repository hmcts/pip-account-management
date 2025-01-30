package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

@RestController
@Tag(name = "Account Management - API for managing list types for subscription")
@RequestMapping("/subscription")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@Valid
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionListTypeController {
    private final SubscriptionService subscriptionService;

    @Autowired
    public SubscriptionListTypeController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/add-list-types/{userId}")
    @Operation(summary = "Endpoint to add list type for existing subscription")
    @ApiResponse(responseCode = "201", description = "Subscription successfully updated for user: {userId}")
    @ApiResponse(responseCode = "400", description =
        "This request object has an invalid format. Please check again.")
    public ResponseEntity<String> addListTypesForSubscription(@PathVariable String userId,
                                                              @RequestBody SubscriptionListType subscriptionListType) {
        subscriptionService.addListTypesForSubscription(subscriptionListType, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format(
                "Location list Type successfully added for user %s",
                userId
            ));
    }

    @PutMapping("/configure-list-types/{userId}")
    @Operation(summary = "Endpoint to update list type for existing subscription")
    @ApiResponse(responseCode = "200", description = "Subscription successfully updated for user: {userId}")
    @ApiResponse(responseCode = "400", description =
        "This request object has an invalid format. Please check again.")
    public ResponseEntity<String> configureListTypesForSubscription(
        @PathVariable String userId, @RequestBody SubscriptionListType subscriptionListType
    ) {
        subscriptionService.configureListTypesForSubscription(subscriptionListType, userId);
        return ResponseEntity.status(HttpStatus.OK)
            .body(String.format(
                "Location list Type successfully updated for user %s",
                userId
            ));
    }
}
