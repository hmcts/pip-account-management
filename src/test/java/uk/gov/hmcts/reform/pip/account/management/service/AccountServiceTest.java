package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.ForbiddenPermissionsException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSubscriber;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AzureUserService azureUserService;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private static final String EMAIL = "a@b.com";
    private static final String INVALID_EMAIL = "ab.com";
    private static final String ID = "1234";
    private static final String ERROR_MESSAGE = "An error has been found";
    private static final String VALIDATION_MESSAGE = "Validation Message";
    private static final String EMAIL_VALIDATION_MESSAGE = "Subscriber should have expected email";
    private static final String ERRORED_ACCOUNTS_VALIDATION_MESSAGE = "Should contain ERRORED_ACCOUNTS key";
    private static final String TEST = "Test";
    private static final String MESSAGES_MATCH = "Messages should match";

    private static final UUID VALID_USER_ID = UUID.randomUUID();
    private static final UUID VALID_USER_ID_IDAM = UUID.randomUUID();

    private final PiUser piUser = new PiUser();
    private final PiUser piUserIdam = new PiUser();

    @BeforeEach
    void setup() {
        piUser.setUserId(VALID_USER_ID);
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setProvenanceUserId(ID);

        piUserIdam.setUserId(VALID_USER_ID_IDAM);
        piUserIdam.setUserProvenance(UserProvenances.CFT_IDAM);
        piUserIdam.setProvenanceUserId(ID);

        lenient().when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(piUser));
        lenient().when(userRepository.findByUserId(VALID_USER_ID_IDAM)).thenReturn(Optional.of(piUserIdam));
    }

    @Test
    void testSubscriberCreated() throws AzureCustomException {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = ID;

        when(validator.validate(argThat(sub -> ((Subscriber) sub).getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(expectedUser);

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.CREATED_ACCOUNTS), "Should contain "
            + "CREATED_ACCOUNTS key");
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, subscriber.getAzureSubscriberId(), "Subscriber should have azure "
            + "object ID");
        assertEquals(0, createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "Map should have no errored accounts"
        );
    }

    @Test
    void testSubscriberNotCreated() throws AzureCustomException {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);

        when(validator.validate(argThat(sub -> ((Subscriber) sub).getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenThrow(new AzureCustomException(ERROR_MESSAGE));

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertNull(subscriber.getAzureSubscriberId(), "Subscriber should have no azure ID set");
        assertEquals(ERROR_MESSAGE, ((ErroredSubscriber) subscribers.get(0)).getErrorMessages().get(0),
                     "Subscriber should have error message set when failed"
        );
        assertEquals(0, createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts"
        );
    }

    @Test
    void testValidationCreatesAnErroredAccount() {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);

        when(validator.validate(argThat(sub -> ((Subscriber) sub).getEmail().equals(subscriber.getEmail()))))
            .thenReturn(Set.of(constraintViolation));

        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertNull(subscriber.getAzureSubscriberId(), "Subscriber should have no azure ID set");

        assertEquals(VALIDATION_MESSAGE, ((ErroredSubscriber) subscribers.get(0)).getErrorMessages().get(0),
                     "Subscriber should have error message set when validation has failed"
        );

        assertEquals(0, createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts"
        );
    }

    @Test
    void creationOfMultipleSubscribers() throws AzureCustomException {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);

        Subscriber erroredSubscriber = new Subscriber();
        erroredSubscriber.setEmail(INVALID_EMAIL);

        User expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = ID;

        doReturn(Set.of()).when(validator).validate(argThat(sub -> ((Subscriber) sub)
            .getEmail().equals(subscriber.getEmail())));

        doReturn(Set.of(constraintViolation)).when(validator).validate(argThat(sub -> ((Subscriber) sub)
            .getEmail().equals(erroredSubscriber.getEmail())));

        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(subscriber.getEmail()))))
            .thenReturn(expectedUser);

        Map<CreationEnum, List<? extends Subscriber>> createdSubscribers =
            accountService.createSubscribers(List.of(subscriber, erroredSubscriber));

        assertTrue(createdSubscribers.containsKey(CreationEnum.CREATED_ACCOUNTS), "Should contain "
            + "CREATED_ACCOUNTS key");
        List<? extends Subscriber> subscribers = createdSubscribers.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(subscriber.getEmail(), subscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);

        assertTrue(createdSubscribers.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends Subscriber> erroredSubscribers = createdSubscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(erroredSubscriber.getEmail(), erroredSubscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(VALIDATION_MESSAGE, ((ErroredSubscriber) erroredSubscribers.get(0)).getErrorMessages().get(0),
                     "Validation message displayed for errored subscriber"
        );

    }

    @Test
    void testAddUsers() {
        PiUser user = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL, Roles.INTERNAL_ADMIN_CTSC);
        Map<CreationEnum, List<?>> expected = new ConcurrentHashMap<>();
        expected.put(CreationEnum.CREATED_ACCOUNTS, List.of(user.getUserId()));
        expected.put(CreationEnum.ERRORED_ACCOUNTS, List.of());

        when(validator.validate(user)).thenReturn(Set.of());
        when(userRepository.save(user)).thenReturn(user);

        assertEquals(expected, accountService.addUsers(List.of(user), EMAIL), "Returned maps should match");
    }

    @Test
    void testAddUsersBuildsErrored() {
        PiUser user = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, INVALID_EMAIL,
                                 Roles.INTERNAL_ADMIN_CTSC);
        ErroredPiUser erroredUser = new ErroredPiUser(user);
        erroredUser.setErrorMessages(List.of(VALIDATION_MESSAGE));
        Map<CreationEnum, List<?>> expected = new ConcurrentHashMap<>();
        expected.put(CreationEnum.CREATED_ACCOUNTS, List.of());
        expected.put(CreationEnum.ERRORED_ACCOUNTS, List.of(erroredUser));

        lenient().when(userRepository.save(user)).thenReturn(user);
        doReturn(Set.of(constraintViolation)).when(validator).validate(user);
        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);

        assertEquals(expected, accountService.addUsers(List.of(user), EMAIL), "Returned Errored accounts should match");
    }

    @Test
    void testAddUsersForBothCreatedAndErrored() {
        PiUser invalidUser = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, INVALID_EMAIL,
                                        Roles.INTERNAL_ADMIN_CTSC
        );
        PiUser validUser = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL, Roles.INTERNAL_ADMIN_CTSC);
        ErroredPiUser erroredUser = new ErroredPiUser(invalidUser);
        erroredUser.setErrorMessages(List.of(VALIDATION_MESSAGE));
        Map<CreationEnum, List<?>> expected = new ConcurrentHashMap<>();
        expected.put(CreationEnum.CREATED_ACCOUNTS, List.of(validUser.getUserId()));
        expected.put(CreationEnum.ERRORED_ACCOUNTS, List.of(erroredUser));

        lenient().when(userRepository.save(invalidUser)).thenReturn(invalidUser);
        doReturn(Set.of(constraintViolation)).when(validator).validate(invalidUser);
        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);

        doReturn(Set.of()).when(validator).validate(validUser);
        when(userRepository.save(validUser)).thenReturn(validUser);

        assertEquals(expected, accountService.addUsers(List.of(invalidUser, validUser), EMAIL),
                     "Returned maps should match created and errored"
        );
    }

    @Test
    void testFindUserByProvenanceId() {
        PiUser user = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL, Roles.INTERNAL_ADMIN_CTSC);
        when(userRepository.findExistingByProvenanceId(user.getProvenanceUserId(), user.getUserProvenance().name()))
            .thenReturn(List.of(user));
        assertEquals(user, accountService.findUserByProvenanceId(user.getUserProvenance(), user.getProvenanceUserId()),
                     "Should return found user");
    }

    @Test
    void testFindUserByProvenanceIdThrows() {
        when(userRepository.findExistingByProvenanceId(TEST, "CRIME_IDAM")).thenReturn(List.of());
        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () ->
            accountService.findUserByProvenanceId(UserProvenances.CRIME_IDAM, TEST));
        assertEquals("No user found with the provenanceUserId: Test", ex.getMessage(), MESSAGES_MATCH);
    }

    @Test
    void testIsUserAuthorisedForPublicationReturnsTrue() {
        assertTrue(accountService.isUserAuthorisedForPublication(VALID_USER_ID, ListType.SJP_PRESS_LIST),
                   "User from PI_AAD should return true for allowed list type");
    }

    @Test
    void testIsUserAuthorisedForPublicationPublicListType() {
        assertTrue(accountService.isUserAuthorisedForPublication(VALID_USER_ID_IDAM, ListType.CIVIL_DAILY_CAUSE_LIST),
                   "Should return true regardless of user provenance if list type has no restrictions");
    }

    @Test
    void testIsUserAuthorisedInvalidUser() {
        UUID invalidId = UUID.randomUUID();
        when(userRepository.findByUserId(invalidId)).thenReturn(Optional.empty());
        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () ->
            accountService.isUserAuthorisedForPublication(invalidId, ListType.CIVIL_DAILY_CAUSE_LIST));
        assertEquals("No user found with the userId: " + invalidId, ex.getMessage(), MESSAGES_MATCH);
    }

    @Test
    void testIsUserAuthorisedNotAuthorised() {
        ForbiddenPermissionsException ex = assertThrows(ForbiddenPermissionsException.class, () ->
            accountService.isUserAuthorisedForPublication(VALID_USER_ID_IDAM, ListType.SJP_PRESS_LIST),
                                                        "Should throw forbidden if user is not "
                                                             + "allowed to see list type");
        assertEquals(String.format("User: %s does not have sufficient permission to view list type: %s",
                                   VALID_USER_ID_IDAM, ListType.SJP_PRESS_LIST), ex.getMessage(),
                     MESSAGES_MATCH);
    }
}
