package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.UserSubscription;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionService;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.UserSubscriptionService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@Tag(name = "Account Management - API for managing account subscriptions")
@RequestMapping("/subscription")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@Valid
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";
    private static final String FORBIDDEN_ERROR_CODE = "403";
    private static final String X_REQUESTER_ID_HEADER = "x-requester-id";

    private final SubscriptionService subscriptionService;
    private final UserSubscriptionService userSubscriptionService;
    private final SubscriptionNotificationService subscriptionNotificationService;

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Endpoint to create a new unique subscription "
        + "- the 'id' field is hidden from swagger as it is auto generated on creation")
    @ApiResponse(responseCode = "201", description = "Subscription successfully created with the id: "
        + "{subscription id} "
        + "for user: {userId}")
    @ApiResponse(responseCode = "400", description = "This subscription object has an invalid format. Please "
        + "check again.")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is not authorised to add subscription with ID {subscriptionId}")
    @PreAuthorize("@subscriptionAuthorisationService.userCanAddSubscriptions(#requesterId, #subscription)")
    public ResponseEntity<String> createSubscription(
        @RequestBody @Valid Subscription subscription,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        Subscription createdSubscription = subscriptionService.createSubscription(subscription, requesterId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Subscription created with the id %s for user %s",
                                createdSubscription.getId(), createdSubscription.getUserId()
            ));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscription: {subId} was deleted")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription id {subId}")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is not authorised to remove these subscriptions")
    @Transactional
    @Operation(summary = "Endpoint to delete a given unique subscription, using subscription ID as a parameter.")
    @DeleteMapping("/{subId}")
    @PreAuthorize("@subscriptionAuthorisationService.userCanDeleteSubscriptions(#requesterId, #subId)")
    public ResponseEntity<String> deleteById(@Parameter @PathVariable UUID subId,
                                             @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId) {
        subscriptionService.deleteById(subId, requesterId);
        return ResponseEntity.ok(String.format("Subscription: %s was deleted", subId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscriptions with IDs {subIds} deleted")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription ID: {subIds}")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is not authorised to remove these subscriptions")
    @Transactional
    @Operation(summary = "Delete a set of subscriptions using the subscription IDs")
    @DeleteMapping("/bulk")
    @PreAuthorize("@subscriptionAuthorisationService.userCanBulkDeleteSubscriptions(#requesterId, #subIds)")
    public ResponseEntity<String> bulkDeleteSubscriptions(@RequestBody List<UUID> subIds,
                                                          @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId) {
        subscriptionService.bulkDeleteSubscriptions(subIds);
        return ResponseEntity.ok(String.format(
            "Subscriptions with ID %s deleted",
            subIds.toString().replace("[", "")
                .replace("]", "")
        ));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscription {subId} found")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription id {subId}")
    @Operation(summary = "Returns the subscription object associated with a given subscription id.")
    @GetMapping("/{subId}")
    public ResponseEntity<Subscription> findBySubId(@Parameter @PathVariable UUID subId) {
        return ResponseEntity.ok(subscriptionService.findById(subId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscriptions list for user id {userId} found")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the user id {userId}")
    @ApiResponse(responseCode = FORBIDDEN_ERROR_CODE,
        description = "User with ID {requesterId} is not authorised to view these subscriptions")
    @Operation(summary = "Returns the list of relevant subscriptions associated with a given user id.")
    @PreAuthorize("@subscriptionAuthorisationService.userCanViewSubscriptions(#requesterId, #userId)")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserSubscription> findByUserId(
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId,
        @Parameter @PathVariable UUID userId) {
        return ResponseEntity.ok(userSubscriptionService.findByUserId(userId));
    }

    @ApiResponse(responseCode = "202", description = "Subscriber request has been accepted")
    @Operation(summary = "Takes in artefact to build subscriber list.")
    @PostMapping("/artefact-recipients")
    public ResponseEntity<String> buildSubscriberList(@RequestBody Artefact artefact) {
        subscriptionNotificationService.collectSubscribers(artefact);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body("Subscriber request has been accepted");
    }

    @ApiResponse(responseCode = "202", description = "Third Parties list deletion accepted")
    @Operation(summary = "Takes in a deleted artefact to notify subscribed third parties")
    @PostMapping("/deleted-artefact")
    public ResponseEntity<String> buildDeletedArtefactSubscribers(@RequestBody Artefact artefact) {
        subscriptionNotificationService.collectThirdPartyForDeletion(artefact);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(
            "Deleted artefact third party subscriber notification request has been accepted");
    }

    @ApiResponse(responseCode = OK_CODE, description = "List of All subscriptions MI data")
    @Operation(summary = "Returns a list of metadata for all existing subscriptions for MI reporting")
    @GetMapping("/mi-data-all")
    @IsAdmin
    public ResponseEntity<List<AllSubscriptionMiData>> getSubscriptionDataForMiReportingAll() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getAllSubscriptionsDataForMiReporting());
    }

    @ApiResponse(responseCode = OK_CODE, description = "List of Location Subscription MI Data")
    @Operation(summary = "Returns a list of subscription data for location-based subscriptions for MI reporting")
    @GetMapping("/mi-data-location")
    @IsAdmin
    public ResponseEntity<List<LocationSubscriptionMiData>> getSubscriptionDataForMiReportingLocation() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getLocationSubscriptionsDataForMiReporting());
    }
}
