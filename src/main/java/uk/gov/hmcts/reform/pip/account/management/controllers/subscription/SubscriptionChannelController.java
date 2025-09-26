package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionChannelService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing subscription channels")
@RequestMapping("/subscription/channel")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@Valid
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionChannelController {
    private static final String X_REQUESTER_ID_HEADER = "x-requester-id";

    private final SubscriptionChannelService subscriptionChannelService;

    @Autowired
    public SubscriptionChannelController(SubscriptionChannelService subscriptionChannelService) {
        this.subscriptionChannelService = subscriptionChannelService;
    }

    @GetMapping
    @Operation(summary = "Endpoint to retrieve the available subscription channels")
    @ApiResponse(responseCode = "200", description = "List of channels returned in JSON array format")
    @ApiResponse(responseCode = "403",
        description = "User with ID {requesterId} is not authorised to retrieve these channel")
    @PreAuthorize("@subscriptionAuthorisationService.userCanRetrieveChannels(#requesterId, #userId)")
    public ResponseEntity<List<Channel>> retrieveChannels(@RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId,
                                                          @RequestParam(name = "userId") String userId) {
        return ResponseEntity.ok(subscriptionChannelService.retrieveChannels());
    }
}
