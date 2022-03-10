package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    private static final String MESSAGES_MATCH = "Status codes should match";

    @Test
    void createSubscriber() {
        Map<CreationEnum, List<? extends Subscriber>> subscribersMap = new ConcurrentHashMap<>();
        subscribersMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new Subscriber()));

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");

        List<Subscriber> subscribers = List.of(subscriber);

        when(accountService.createSubscribers(argThat(arg -> arg.equals(subscribers)))).thenReturn(subscribersMap);

        ResponseEntity<Map<CreationEnum, List<? extends Subscriber>>> response =
            accountController.createSubscriber(subscribers);

        assertEquals(HttpStatus.OK, response.getStatusCode(), MESSAGES_MATCH);
        assertEquals(subscribersMap, response.getBody(), "Should return the expected subscribers map");
    }

    @Test
    void testCreateUser() {
        Map<CreationEnum, List<?>> usersMap = new ConcurrentHashMap<>();
        usersMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new PiUser()));

        PiUser user = new PiUser();
        user.setEmail("a@b.com");

        List<PiUser> users = List.of(user);

        when(accountService.addUsers(users, "test")).thenReturn(usersMap);

        ResponseEntity<Map<CreationEnum, List<?>>> response = accountController.createUsers("test", users);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                     MESSAGES_MATCH);
        assertEquals(usersMap, response.getBody(), "Should return the expected user map");
    }

    @Test
    void testIsUserAuthorised() {
        when(accountService.isUserAuthorisedForPublication(any(), any())).thenReturn(true);
        assertEquals(HttpStatus.OK,
                     accountController.isUserAuthorised(UUID.randomUUID(), ListType.MAGS_PUBLIC_LIST).getStatusCode(),
                     MESSAGES_MATCH);
        assertEquals(true,
                     accountController.isUserAuthorised(UUID.randomUUID(), ListType.MAGS_PUBLIC_LIST).getBody(),
                     "Should return boolean value");
    }

}
