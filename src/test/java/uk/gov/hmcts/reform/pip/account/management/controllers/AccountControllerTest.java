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
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String EMAIL = "a@b.com";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    void createSubscriber() {
        Map<CreationEnum, List<? extends Subscriber>> subscribersMap = new ConcurrentHashMap<>();
        subscribersMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new Subscriber()));

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);

        List<Subscriber> subscribers = List.of(subscriber);

        when(accountService.createSubscribers(argThat(arg -> arg.equals(subscribers)))).thenReturn(subscribersMap);

        ResponseEntity<Map<CreationEnum, List<? extends Subscriber>>> response =
            accountController.createSubscriber(subscribers);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(subscribersMap, response.getBody(), "Should return the expected subscribers map");
    }

    @Test
    void testCreateUser() {
        Map<CreationEnum, List<?>> usersMap = new ConcurrentHashMap<>();
        usersMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new PiUser()));

        PiUser user = new PiUser();
        user.setEmail(EMAIL);

        List<PiUser> users = List.of(user);

        when(accountService.addUsers(users, "test")).thenReturn(usersMap);

        ResponseEntity<Map<CreationEnum, List<?>>> response = accountController.createUsers("test", users);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                     STATUS_CODE_MATCH);

        assertEquals(usersMap, response.getBody(), "Should return the expected user map");
    }

    @Test
    void testIsUserAuthorised() {
        when(accountService.isUserAuthorisedForPublication(any(), any())).thenReturn(true);
        assertEquals(
            HttpStatus.OK,
            accountController.checkUserAuthorised(UUID.randomUUID(), ListType.MAGS_PUBLIC_LIST).getStatusCode(),
            STATUS_CODE_MATCH
        );
        assertEquals(
            true,
            accountController.checkUserAuthorised(UUID.randomUUID(), ListType.MAGS_PUBLIC_LIST).getBody(),
            "Should return boolean value"
        );
    }

    @Test
    void testGetUserByProvenanceId() {
        PiUser user = new PiUser();
        user.setProvenanceUserId("123");
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setEmail(EMAIL);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        when(accountService.findUserByProvenanceId(user.getUserProvenance(), user.getProvenanceUserId()))
            .thenReturn(user);

        assertEquals(user,
                     accountController.getUserByProvenanceId(user.getUserProvenance(), user.getProvenanceUserId())
                         .getBody(), "Should return found user");

        assertEquals(HttpStatus.OK, accountController.getUserByProvenanceId(user.getUserProvenance(),
                                                                            user.getProvenanceUserId()).getStatusCode(),
                     STATUS_CODE_MATCH);
    }

}
