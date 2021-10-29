package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;

import java.util.List;
import java.util.Map;
import javax.validation.Valid;

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
    public ResponseEntity<Map<CreationEnum, List<? extends Subscriber>>> createSubscriber(
        @RequestBody List<Subscriber> subscribers) {
        Map<CreationEnum, List<? extends Subscriber>> processedSubscribers =
            accountService.createSubscribers(subscribers);
        return ResponseEntity.ok(processedSubscribers);
    }


}
