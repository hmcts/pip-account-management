package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionChannelService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.util.List;

@RestController
@Tag(name = "Subscription Channel API")
@RequestMapping("/subscription/channel")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403", description = "User has not been authorized")
@Valid
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionChannelController {
    private final SubscriptionChannelService subscriptionChannelService;

    @Autowired
    public SubscriptionChannelController(SubscriptionChannelService subscriptionChannelService) {
        this.subscriptionChannelService = subscriptionChannelService;
    }

    @GetMapping
    @Operation(summary = "Endpoint to retrieve the available subscription channels")
    @ApiResponse(responseCode = "200", description = "List of channels returned in JSON array format")
    public ResponseEntity<List<Channel>> retrieveChannels() {
        return ResponseEntity.ok(subscriptionChannelService.retrieveChannels());
    }


}
