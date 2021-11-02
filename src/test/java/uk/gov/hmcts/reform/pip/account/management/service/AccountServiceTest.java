package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ErroredSubscriber;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AzureUserService azureUserService;

    @Mock
    private AzureTableService azureTableService;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @InjectMocks
    private AccountService accountService;

    private static final String SUBSCRIBER_EMAIL = "a@b.com";
    private static final String SUBSCRIBER_INVALID_EMAIL = "ab.com";
    private static final String ID = "1234";
    private static final String ERROR_MESSAGE = "An error has been found";
    private static final String VALIDATION_MESSAGE = "Validation Message";
    private static final String EMAIL_VALIDATION_MESSAGE = "Subscriber should have expected email";
    private static final String ERRORED_ACCOUNTS_VALIDATION_MESSAGE = "Should contain ERRORED_ACCOUNTS key";

    @Test
    void testSubscriberCreated() throws AzureCustomException {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(SUBSCRIBER_EMAIL);

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = ID;

        String expectedString = "abcd";

        when(validator.validate(argThat(sub -> ((Subscriber)sub).getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(expectedUser);

        when(azureTableService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(expectedString);

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.CREATED_ACCOUNTS), "Should contain "
            + "CREATED_ACCOUNTS key");
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, subscriber.getAzureSubscriberId(), "Subscriber should have azure "
            + "object ID");
        assertEquals(expectedString, subscriber.getTableSubscriberId(), "Subscriber should have table "
            + "ID");
        assertEquals(0, createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "Map should have no errored accounts");
    }

    @Test
    void testSubscriberPartiallyCreated() throws AzureCustomException {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(SUBSCRIBER_EMAIL);

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = ID;

        when(validator.validate(argThat(sub -> ((Subscriber)sub).getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(expectedUser);

        when(azureTableService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenThrow(new AzureCustomException(ERROR_MESSAGE));

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals("1234", subscriber.getAzureSubscriberId(), "Subscriber should have azure "
            + "object ID");
        assertNull(subscriber.getTableSubscriberId(), "Subscriber should have no table ID set");
        assertEquals(ERROR_MESSAGE, ((ErroredSubscriber)subscribers.get(0)).getErrorMessages().get(0),
                     "Subscriber should have error message set when failed");
        assertEquals(0, createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts");
    }

    @Test
    void testSubscriberNotCreated() throws AzureCustomException {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(SUBSCRIBER_EMAIL);

        when(validator.validate(argThat(sub -> ((Subscriber)sub).getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenThrow(new AzureCustomException(ERROR_MESSAGE));

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertNull(subscriber.getAzureSubscriberId(), "Subscriber should have no azure ID set");
        assertNull(subscriber.getTableSubscriberId(), "Subscriber should have no table ID set");
        assertEquals(ERROR_MESSAGE, ((ErroredSubscriber)subscribers.get(0)).getErrorMessages().get(0),
                     "Subscriber should have error message set when failed");
        assertEquals(0, createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts");
    }

    @Test
    void testValidationCreatesAnErroredAccount() {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(SUBSCRIBER_EMAIL);

        when(validator.validate(argThat(sub -> ((Subscriber)sub).getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Set.of(constraintViolation));

        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertNull(subscriber.getAzureSubscriberId(), "Subscriber should have no azure ID set");
        assertNull(subscriber.getTableSubscriberId(), "Subscriber should have no table ID set");

        assertEquals(VALIDATION_MESSAGE, ((ErroredSubscriber)subscribers.get(0)).getErrorMessages().get(0),
                     "Subscriber should have error message set when validation has failed");

        assertEquals(0, createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts");
    }

    @Test
    void creationOfMultipleSubscribers() throws AzureCustomException {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(SUBSCRIBER_EMAIL);

        Subscriber erroredSubscriber = new Subscriber();
        erroredSubscriber.setEmail(SUBSCRIBER_INVALID_EMAIL);

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = ID;

        String expectedString = "abcd";

        doReturn(Set.of()).when(validator).validate(argThat(sub -> ((Subscriber)sub)
            .getEmail().equals(subscriber.getEmail())));

        doReturn(Set.of(constraintViolation)).when(validator).validate(argThat(sub -> ((Subscriber)sub)
            .getEmail().equals(erroredSubscriber.getEmail())));

        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(expectedUser);

        when(azureTableService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(expectedString);

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber, erroredSubscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.CREATED_ACCOUNTS), "Should contain "
            + "CREATED_ACCOUNTS key");
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends Subscriber> erroredSubscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(erroredSubscriber.getEmail(), erroredSubscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(VALIDATION_MESSAGE, ((ErroredSubscriber)erroredSubscribers.get(0)).getErrorMessages().get(0),
                     "Validation message displayed for errored subscriber");

    }

}
