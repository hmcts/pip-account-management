package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@Tag(name = "Account Management - API for managing account subscriptions")
@RequestMapping("/subscription")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@Valid
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {

    private static final String OK_CODE = "200";
    private static final String NOT_FOUND_ERROR_CODE = "404";
    private static final String X_USER_ID_HEADER = "x-user-id";

    private final SubscriptionService subscriptionService;
    private final UserSubscriptionService userSubscriptionService;
    private final SubscriptionNotificationService subscriptionNotificationService;

    @Autowired
    public SubscriptionController(
        SubscriptionService subscriptionService,
        UserSubscriptionService userSubscriptionService,
        SubscriptionNotificationService subscriptionNotificationService
    ) {
        this.subscriptionService = subscriptionService;
        this.userSubscriptionService = userSubscriptionService;
        this.subscriptionNotificationService = subscriptionNotificationService;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Endpoint to create a new unique subscription "
        + "- the 'id' field is hidden from swagger as it is auto generated on creation")
    @ApiResponse(responseCode = "201", description = "Subscription successfully created with the id: "
        + "{subscription id} "
        + "for user: {userId}")
    @ApiResponse(responseCode = "400", description = "This subscription object has an invalid format. Please "
        + "check again.")
    public ResponseEntity<String> createSubscription(
        @RequestBody @Valid Subscription subscription,
        @RequestHeader(X_USER_ID_HEADER) String actioningUserId
    ) {
        Subscription createdSubscription = subscriptionService.createSubscription(subscription, actioningUserId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(String.format("Subscription created with the id %s for user %s",
                                createdSubscription.getId(), createdSubscription.getUserId()
            ));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscription: {subId} was deleted")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription id {subId}")
    @Transactional
    @Operation(summary = "Endpoint to delete a given unique subscription, using subscription ID as a parameter.")
    @DeleteMapping("/{subId}")
    @PreAuthorize("@authorisationService.userCanDeleteSubscriptions(#actioningUserId, #subId)")
    public ResponseEntity<String> deleteById(@Parameter @PathVariable UUID subId,
                                             @RequestHeader(X_USER_ID_HEADER) String actioningUserId) {

        subscriptionService.deleteById(subId, actioningUserId);
        return ResponseEntity.ok(String.format("Subscription: %s was deleted", subId));
    }

    @ApiResponse(responseCode = OK_CODE, description = "Subscriptions with IDs {subIds} deleted")
    @ApiResponse(responseCode = NOT_FOUND_ERROR_CODE,
        description = "No subscription found with the subscription ID: {subIds}")
    @Transactional
    @Operation(summary = "Delete a set of subscriptions using the subscription IDs")
    @DeleteMapping("/bulk")
    @PreAuthorize("@authorisationService.userCanDeleteSubscriptions(#actioningUserId, #subIds)")
    public ResponseEntity<String> bulkDeleteSubscriptions(@RequestBody List<UUID> subIds,
                                                          @RequestHeader(X_USER_ID_HEADER) String actioningUserId) {

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
    @Operation(summary = "Returns the list of relevant subscriptions associated with a given user id.")
    @GetMapping("/user/{userId}")
    public ResponseEntity<UserSubscription> findByUserId(@Parameter @PathVariable UUID userId) {
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

    /**
     * Previous version of the MI Reporting endpoint. No longer used and soon to be removed.
     * @deprecated This endpoint will be removed in the future in favour of the V2 equivalent.
     */
    @ApiResponse(responseCode = OK_CODE, description = "A CSV like structure which contains the data. "
        + "See example for headers ", content = {
            @Content(examples = {@ExampleObject("id,channel,search_type,user_id,court_name,created_date")},
                mediaType = MediaType.TEXT_PLAIN_VALUE,
                schema = @Schema(implementation = String.class))
            }
        )
    @Operation(summary = "Returns a list of metadata for all existing subscriptions for MI reporting. "
        + "This endpoint will be deprecated in the future, in favour of returning a JSON model")
    @GetMapping("/mi-data-all")
    @IsAdmin
    @Deprecated(since = "2")
    public ResponseEntity<String> getSubscriptionDataForMiReportingAll() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getAllSubscriptionsDataForMiReporting());
    }

    @ApiResponse(responseCode = OK_CODE, description = "List of All subscriptions MI data")
    @Operation(summary = "Returns a list of metadata for all existing subscriptions for MI reporting")
    @GetMapping("/v2/mi-data-all")
    @IsAdmin
    public ResponseEntity<List<AllSubscriptionMiData>> getSubscriptionDataForMiReportingAllV2() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getAllSubscriptionsDataForMiReportingV2());
    }

    /**
     * Previous version of the MI Reporting endpoint. No longer used and soon to be removed.
     * @deprecated This endpoint will be removed in the future in favour of the V2 equivalent.
     */
    @ApiResponse(responseCode = OK_CODE, description = "A CSV like structure which contains the data. "
        + "See example for headers ", content = {
            @Content(examples = {@ExampleObject("id,search_value,channel,user_id,court_name,created_date")},
                mediaType = MediaType.TEXT_PLAIN_VALUE,
                schema = @Schema(implementation = String.class))
            }
        )
    @Operation(summary = "Returns a list of subscription data for location-based subscriptions for MI reporting. "
        + "This endpoint will be deprecated in the future, in favour of returning a JSON model")
    @GetMapping("/mi-data-local")
    @IsAdmin
    @Deprecated(since = "2")
    public ResponseEntity<String> getSubscriptionDataForMiReportingLocal() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getLocalSubscriptionsDataForMiReporting());
    }

    @ApiResponse(responseCode = OK_CODE, description = "List of Location Subscription MI Data")
    @Operation(summary = "Returns a list of subscription data for location-based subscriptions for MI reporting")
    @GetMapping("/v2/mi-data-location")
    @IsAdmin
    public ResponseEntity<List<LocationSubscriptionMiData>> getSubscriptionDataForMiReportingLocationV2() {
        return ResponseEntity.status(HttpStatus.OK)
            .body(subscriptionService.getLocationSubscriptionsDataForMiReportingV2());
    }
}
