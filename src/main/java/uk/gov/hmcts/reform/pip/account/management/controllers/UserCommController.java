package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.UserComm;
import uk.gov.hmcts.reform.pip.account.management.service.UserCommService;
import uk.gov.hmcts.reform.pip.model.authentication.roles.IsAdmin;

import java.util.List;

@RestController
@Tag(name = "Account Management - API for managing user communications")
@RequestMapping("/comm")
@IsAdmin
@AllArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserCommController {
    private final UserCommService userCommService;

    @Operation(summary = "Get all current user communications")
    @GetMapping()
    public ResponseEntity<List<UserComm>> getAllUserComms() {
        return ResponseEntity.ok(userCommService.getAll());
    }
}
