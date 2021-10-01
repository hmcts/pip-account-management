package uk.gov.hmcts.reform.rsecheck.service;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.demo.model.CreationEnum;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.service.AccountService;
import uk.gov.hmcts.reform.demo.service.AzureTableService;
import uk.gov.hmcts.reform.demo.service.AzureUserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AzureUserService azureUserService;

    @Mock
    private AzureTableService azureTableService;

    @InjectMocks
    private AccountService accountService;

    @Test
    void testSubscriberCreated() {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = "1234";

        String expectedString = "abcd";

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Optional.of(expectedUser));

        when(azureTableService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Optional.of(expectedString));

        Map<CreationEnum, List<Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.CREATED_ACCOUNTS), "Should contain "
            + "CREATED_ACCOUNTS key");
        List<Subscriber> subscribers = createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), "Subscriber should have "
            + "expected email");
        assertEquals("1234", subscriber.getAzureSubscriberId(), "Subscriber should have azure "
            + "object ID");
        assertEquals(expectedString, subscriber.getTableSubscriberId(), "Subscriber should have table "
            + "ID");
        assertTrue(createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS).size() == 0, "Map should "
            + "have no errored accounts");
    }

    @Test
    void testSubscriberPartiallyCreated() {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = "1234";

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Optional.of(expectedUser));

        when(azureTableService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Optional.empty());

        Map<CreationEnum, List<Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), "Should contain"
            + "ERRORED_ACCOUNTS key");
        List<Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), "Subscriber should have "
            + "expected email");
        assertEquals("1234", subscriber.getAzureSubscriberId(), "Subscriber should have azure "
            + "object ID");
        assertNull(subscriber.getTableSubscriberId(), "Subscriber should have no table ID set");
        assertTrue(createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size() == 0, "Map should "
            + "have no created accounts");
    }

    @Test
    void testSubscriberNotCreated() {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Optional.empty());

        Map<CreationEnum, List<Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), "Should contain"
            + "ERRORED_ACCOUNTS key");
        List<Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), "Subscriber should have "
            + "expected email");
        assertNull(subscriber.getAzureSubscriberId(), "Subscriber should have no azure ID set");
        assertNull(subscriber.getTableSubscriberId(), "Subscriber should have no table ID set");
        assertTrue(createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size() == 0, "Map should "
            + "have no created accounts");
    }

}
