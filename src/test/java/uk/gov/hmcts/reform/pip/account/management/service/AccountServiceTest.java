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
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class AccountServiceTest {

    @Mock
    private AzureUserService azureUserService;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @Mock
    private Path path;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PublicationService publicationService;

    @InjectMocks
    private AccountService accountService;

    private static final String ISSUER_EMAIL = "b@c.com";
    private static final String EMAIL = "a@b.com";
    private static final String INVALID_EMAIL = "ab.com";
    private static final String ID = "1234";
    private static final String EMAIL_PATH = "Email";
    private static final String ERROR_MESSAGE = "An error has been found";
    private static final String VALIDATION_MESSAGE = "Validation Message";
    private static final String EMAIL_VALIDATION_MESSAGE = "AzureAccount should have expected email";
    private static final String ERRORED_ACCOUNTS_VALIDATION_MESSAGE = "Should contain ERRORED_ACCOUNTS key";
    private static final String TEST = "Test";
    private static final String MESSAGES_MATCH = "Messages should match";

    private static final UUID VALID_USER_ID = UUID.randomUUID();
    private static final UUID VALID_USER_ID_IDAM = UUID.randomUUID();

    private final PiUser piUser = new PiUser();
    private final PiUser piUserIdam = new PiUser();
    private AzureAccount azureAccount;
    private User expectedUser;

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

    @BeforeEach
    void beforeEach() {
        azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);

        expectedUser = new User();
        expectedUser.givenName = "Test";
        expectedUser.id = ID;

        lenient().when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);
        lenient().when(constraintViolation.getPropertyPath()).thenReturn(path);
        lenient().when(path.toString()).thenReturn(EMAIL_PATH);
    }


    @Test
    void testAccountCreated() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(expectedUser);

        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn("test");

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_EMAIL);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), "Should contain "
            + "CREATED_ACCOUNTS key");
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, azureAccount.getAzureAccountId(), "AzureAccount should have azure "
            + "object ID");
        assertEquals(0, createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "Map should have no errored accounts"
        );
    }

    @Test
    void testAccountNotCreated() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail()))))
            .thenThrow(new AzureCustomException(ERROR_MESSAGE));

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_EMAIL);

        assertTrue(createdAccounts.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertNull(azureAccount.getAzureAccountId(), "AzureAccount should have no azure ID set");
        assertEquals(ERROR_MESSAGE, ((ErroredAzureAccount) accounts.get(0)).getErrorMessages().get(0),
                     "Account should have error message set when failed"
        );
        assertEquals(0, createdAccounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts"
        );
    }

    @Test
    void testValidationCreatesAnErroredAccount() {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of(constraintViolation));

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_EMAIL);

        assertTrue(createdAccounts.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertNull(azureAccount.getAzureAccountId(), "Account should have no azure ID set");

        assertEquals(
            EMAIL_PATH + ": " + VALIDATION_MESSAGE,
            ((ErroredAzureAccount) accounts.get(0)).getErrorMessages().get(0),
            "Account should have error message set when validation has failed"
        );

        assertEquals(0, createdAccounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts"
        );
    }

    @Test
    void creationOfMultipleAccounts() throws AzureCustomException {
        AzureAccount erroredAzureAccount = new AzureAccount();
        erroredAzureAccount.setEmail(INVALID_EMAIL);

        doReturn(Set.of()).when(validator).validate(argThat(sub -> ((AzureAccount) sub)
            .getEmail().equals(azureAccount.getEmail())));

        doReturn(Set.of(constraintViolation)).when(validator).validate(argThat(sub -> ((AzureAccount) sub)
            .getEmail().equals(erroredAzureAccount.getEmail())));

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(expectedUser);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount, erroredAzureAccount), ISSUER_EMAIL);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), "Should contain "
            + "CREATED_ACCOUNTS key");
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);

        assertTrue(createdAccounts.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends AzureAccount> erroredSubscribers = createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(erroredAzureAccount.getEmail(), erroredSubscribers.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(
            EMAIL_PATH + ": " + VALIDATION_MESSAGE,
            ((ErroredAzureAccount) erroredSubscribers.get(0)).getErrorMessages().get(0),
            "Validation message displayed for errored azureAccount"
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
                                 Roles.INTERNAL_ADMIN_CTSC
        );
        ErroredPiUser erroredUser = new ErroredPiUser(user);
        erroredUser.setErrorMessages(List.of(VALIDATION_MESSAGE));
        Map<CreationEnum, List<?>> expected = new ConcurrentHashMap<>();
        expected.put(CreationEnum.CREATED_ACCOUNTS, List.of());
        expected.put(CreationEnum.ERRORED_ACCOUNTS, List.of(erroredUser));

        lenient().when(userRepository.save(user)).thenReturn(user);
        doReturn(Set.of(constraintViolation)).when(validator).validate(user);

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
                     "Should return found user"
        );
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
        assertTrue(
            accountService.isUserAuthorisedForPublication(VALID_USER_ID, ListType.SJP_PRESS_LIST),
            "User from PI_AAD should return true for allowed list type"
        );
    }

    @Test
    void testIsUserAuthorisedForPublicationPublicListType() {
        assertTrue(
            accountService.isUserAuthorisedForPublication(VALID_USER_ID_IDAM, ListType.CIVIL_DAILY_CAUSE_LIST),
            "Should return true regardless of user provenance if list type has no restrictions"
        );
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
            + "allowed to see list type"
        );
        assertEquals(String.format("User: %s does not have sufficient permission to view list type: %s",
                                   VALID_USER_ID_IDAM, ListType.SJP_PRESS_LIST
                     ), ex.getMessage(),
                     MESSAGES_MATCH
        );
    }
}
