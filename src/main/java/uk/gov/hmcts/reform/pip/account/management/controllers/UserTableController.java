package uk.gov.hmcts.reform.pip.account.management.controllers;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javassist.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.CftUser;
import uk.gov.hmcts.reform.pip.account.management.service.UserService;

import java.util.UUID;


@RestController
@Api(tags = "Account Management - API for managing user table")
public class UserTableController {

    @Autowired
    UserService userService;

    @PostMapping(consumes= "application/json")
    @ApiOperation("Endpoint to create a new cftUser within the cftUser table. uuid will be generated upon creation")
    @ApiResponses({
        @ApiResponse(code=200, message="CftUser added to cftUser table"),
        @ApiResponse(code=400, message="This cftUser object has an invalid format, please try again")
    })
    public ResponseEntity<String> createUser(@RequestBody CftUser cftUser){

        userService.createUser(cftUser);

        return ResponseEntity.ok(String.format("CftUser created with the uuid %s",
                                               cftUser.getId()));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "User {id} found"),
        @ApiResponse(code=404, message = "No user found with id {id}")
    })
    @Transactional
    @ApiOperation("Endpoint to delete a given CFTUser using the uuid for that user.")
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteById(@ApiParam(value="The specific uuid that should be deleted.",
        required = true) @PathVariable UUID id) throws NotFoundException {
        userService.deleteById(id);
        return ResponseEntity.ok(String.format("User with uuid %s deleted.", id));
    }

    @ApiResponses({
        @ApiResponse(code = 200, message = "User {id} found"),
        @ApiResponse(code=404, message = "No user found with id {id}")
    })
    @ApiOperation("Endpoint to get a given user with their UUID")
    @GetMapping("/{id}")
    public ResponseEntity<CftUser> getById(@ApiParam(value="the specific uuid that should be found.",
        required=true) @PathVariable UUID id) throws NotFoundException {
        CftUser cftUser = userService.findById(id);
        return ResponseEntity.ok(cftUser);
    }


}
