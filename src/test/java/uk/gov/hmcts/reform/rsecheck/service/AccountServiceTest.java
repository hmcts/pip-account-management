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
import uk.gov.hmcts.reform.demo.service.AzureUserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AccountServiceTest {

    @Mock
    private AzureUserService azureUserService;

    @InjectMocks
    private AccountService accountService;

    @Test
    public void testSubscriberCreated() {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = "1234";

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Optional.of(expectedUser));

        Map<CreationEnum, List<Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.CREATED_ACCOUNTS));
        List<Subscriber> subscribers = createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail());
        assertEquals("1234", subscriber.getSubscriberObjectId());
        assertEquals(0, createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS).size());
    }

    @Test
    public void testSubscriberNotCreated() {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("a@b.com");

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Optional.empty());

        Map<CreationEnum, List<Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS));
        List<Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail());
        assertNull(subscriber.getSubscriberObjectId());
        assertEquals(0, createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size());

    }



}
