package uk.gov.hmcts.reform.rsecheck.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.demo.controllers.AccountController;
import uk.gov.hmcts.reform.demo.model.CreationEnum;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.service.AccountService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    public void createSubscriber() {
        Map<CreationEnum, List<Subscriber>> subscribersMap = new HashMap<>();
        subscribersMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new Subscriber()));

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");

        List<Subscriber> subscribers = List.of(subscriber);

        when(accountService.createSubscribers(argThat(arg -> arg.equals(subscribers)))).thenReturn(subscribersMap);

        ResponseEntity<Map<CreationEnum, List<Subscriber>>> response = accountController.createSubscriber(subscribers);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(subscribersMap, response.getBody());
    }

}
