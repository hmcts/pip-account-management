package uk.gov.hmcts.reform.demo.controllers;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.model.CreationEnum;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.service.AccountService;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@Api(tags = "Account Management - API for managing accounts")
@RequestMapping("/account")
@Validated
public class AccountController {

    @Autowired
    private AccountService accountService;

    /**
     * POST endpoint to create a new subscriber account.
     * This will also trigger any welcome emails.
     *
     * @param subscribers The subscribers to add.
     * @return The ID for the subscriber.
     */
    @PostMapping("/add")
    public ResponseEntity<Map<CreationEnum, List<Subscriber>>> createSubscriber(@Valid @RequestBody List<Subscriber> subscribers) {
        Map<CreationEnum, List<Subscriber>> processedSubscribers = accountService.createSubscribers(subscribers);
        return ResponseEntity.ok(processedSubscribers);
    }


}
