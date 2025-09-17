package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionLocationService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;

@RestController
@Tag(name = "Account Management - API for managing subscriptions for location")
@RequestMapping("/subscription/location")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@Valid
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionLocationController {
    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";
    private static final String FORBIDDEN_ERROR_CODE = "403";
    private static final String X_REQUESTER_ID_HEADER = "x-requester-id";

    private final SubscriptionLocationService subscriptionLocationService;

    @Autowired
    public SubscriptionLocationController(SubscriptionLocationService subscriptionLocationService) {
        this.subscriptionLocationService = subscriptionLocationService;
    }

    @ApiResponse(responseCode = OK_CODE, description =
        "Subscriptions list for location id {locationId} found")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description =
        "No subscription found with the location id {locationId}")
    @GetMapping("/{locationId}")
    public ResponseEntity<List<Subscription>> findSubscriptionsByLocationId(
        @PathVariable String locationId) {
        return ResponseEntity.ok(subscriptionLocationService.findSubscriptionsByLocationId(locationId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscription for location {locationId} has been deleted")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE, description = "No subscription found for location {locationId}")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is not authorised to remove these subscriptions")
    @Transactional
    @PreAuthorize("@subscriptionAuthorisationService.userCanDeleteLocationSubscriptions(#requesterId, #locationId)")
    @DeleteMapping("/{locationId}")
    public ResponseEntity<String> deleteSubscriptionByLocation(@RequestHeader(X_REQUESTER_ID_HEADER) String requesterId,
                                                               @PathVariable Integer locationId) {
        return ResponseEntity.ok(subscriptionLocationService.deleteSubscriptionByLocation(
            locationId.toString(),
            requesterId
        ));
    }
}
