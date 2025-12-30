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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartyUserService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Account Management - API for managing third-party users")
@RequestMapping("/third-party")
@IsAdmin
@SecurityRequirement(name = "bearerAuth")
@ApiResponse(responseCode = "401", description = "Invalid access credential")
@ApiResponse(responseCode = "403",
    description = "User with ID {requesterId} is not authorised to manage third-party users")
public class ThirdPartyUserController {
    private static final String OK_STATUS_CODE = "200";
    private static final String CREATED_STATUS_CODE = "201";
    private static final String BAD_REQUEST_STATUS_CODE = "400";
    private static final String NOT_FOUND_STATUS_CODE = "404";
    private static final String X_REQUESTER_ID_HEADER = "x-requester-id";

    private final ThirdPartyUserService thirdPartyUserService;

    @Autowired
    public ThirdPartyUserController(ThirdPartyUserService thirdPartyUserService) {
        this.thirdPartyUserService = thirdPartyUserService;
    }

    @PostMapping(consumes = "application/json")
    @Operation(summary = "Endpoint to create a new third-party user")
    @ApiResponse(responseCode = CREATED_STATUS_CODE,
        description = "third-party user successfully created with ID {userId}")
    @ApiResponse(responseCode = BAD_REQUEST_STATUS_CODE, description = "The third-party user has an invalid format")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<ApiUser> createThirdPartyUser(
        @RequestBody ApiUser apiUser,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        ApiUser createdApiUser = thirdPartyUserService.createThirdPartyUser(apiUser);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(createdApiUser);
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "Endpoint to retrieve all third-party users")
    @ApiResponse(responseCode = OK_STATUS_CODE, description = "All third-party users retrieved")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<List<ApiUser>> getAllThirdPartyUsers(
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        return ResponseEntity.ok(thirdPartyUserService.getAllThirdPartyUsers());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Endpoint to retrieve a third-party user by their ID")
    @ApiResponse(responseCode = OK_STATUS_CODE, description = "third-party user with ID {userId} retrieved")
    @ApiResponse(responseCode = NOT_FOUND_STATUS_CODE, description = "User with ID {userId} not found")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    public ResponseEntity<ApiUser> getThirdPartyUserByUserId(
        @PathVariable UUID userId,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        return ResponseEntity.ok(thirdPartyUserService.findThirdPartyUser(userId));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Endpoint to delete a third-party user and associated subscriptions and configurations")
    @ApiResponse(responseCode = OK_STATUS_CODE, description = "third-party user with ID {userId} deleted")
    @ApiResponse(responseCode = NOT_FOUND_STATUS_CODE, description = "User with ID {userId} not found")
    @PreAuthorize("@thirdPartyAuthorisationService.userCanManageThirdParty(#requesterId)")
    @Transactional
    public ResponseEntity<String> deleteThirdPartyUser(
        @PathVariable UUID userId,
        @RequestHeader(X_REQUESTER_ID_HEADER) UUID requesterId
    ) {
        thirdPartyUserService.deleteThirdPartyUser(userId);
        return ResponseEntity.ok(String.format("Third-party user with ID %s has been deleted", userId));
    }
}
