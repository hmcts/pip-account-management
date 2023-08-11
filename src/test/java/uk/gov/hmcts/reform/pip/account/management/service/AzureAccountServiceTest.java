package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("PMD.TooManyMethods")
class AzureAccountServiceTest {
    private static final String FULL_NAME = "Full name";
    private static final String ISSUER_ID = "abcdef";
    private static final String EMAIL = "test@hmcts.net";
    private static final String INVALID_EMAIL = "ab.com";
    private static final String ID = "1234";
    private static final String EMAIL_PATH = "Email";
    private static final String TEST = "Test";
    private static final boolean FALSE = false;
    private static final boolean TRUE = true;
    private static final String SHOULD_CONTAIN = "Should contain ";
    private static final String SURNAME = "Surname";
    private static final UUID VALID_USER_ID = UUID.randomUUID();

    private final PiUser piUser = new PiUser();
    private final AzureAccount azureAccount = new AzureAccount();
    private final User expectedUser = new User();

    private static final String ERROR_MESSAGE = "An error has been found";
    private static final String VALIDATION_MESSAGE = "Validation Message";
    private static final String EMAIL_VALIDATION_MESSAGE = "AzureAccount should have expected email";
    private static final String ERRORED_ACCOUNTS_VALIDATION_MESSAGE = "Should contain ERRORED_ACCOUNTS key";
    private static final String CREATED_ACCOUNTS_KEY = "CREATED_ACCOUNTS key";
    private static final String ERRORED_ACCOUNTS_KEY = "ERRORED_ACCOUNTS key";
    private static final String RETURN_USER_ERROR = "Returned user does not match expected user";
    private static final String AZURE_ACCOUNT_ERROR = "AzureAccount should have azure object ID";

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
    private AzureAccountService azureAccountService;

    @BeforeEach
    void setup() {
        piUser.setUserId(VALID_USER_ID);
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setProvenanceUserId(ID);
        piUser.setEmail(EMAIL);

        azureAccount.setEmail(EMAIL);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        expectedUser.givenName = TEST;
        expectedUser.id = ID;

        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(piUser));
        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);
        when(constraintViolation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn(EMAIL_PATH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INTERNAL_ADMIN_CTSC", "INTERNAL_SUPER_ADMIN_CTSC"})
    void testAccountCreated(String role) throws AzureCustomException {
        azureAccount.setRole(Roles.valueOf(role));

        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.getUser(EMAIL)).thenReturn(null);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);

        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn(TRUE);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN + CREATED_ACCOUNTS_KEY);
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, azureAccount.getAzureAccountId(), AZURE_ACCOUNT_ERROR);
        assertEquals(0, createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "Map should have no errored accounts"
        );

        verify(azureUserService).createUser(azureAccount, FALSE);
    }

    @Test
    void testVerifiedAccountCreated() throws AzureCustomException {
        azureAccount.setRole(Roles.VERIFIED);

        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.getUser(EMAIL)).thenReturn(null);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);

        when(publicationService.sendMediaNotificationEmail(azureAccount.getEmail(), TEST, false))
            .thenReturn(TRUE);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
            + CREATED_ACCOUNTS_KEY);
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, azureAccount.getAzureAccountId(), AZURE_ACCOUNT_ERROR);
        assertEquals(0, createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "Map should have no errored accounts"
        );

        verify(azureUserService).createUser(azureAccount, FALSE);
    }

    @Test
    void testVerifiedAccountCreatedWithSuppliedPassword() throws AzureCustomException {
        azureAccount.setRole(Roles.VERIFIED);

        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.getUser(EMAIL)).thenReturn(null);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);

        when(publicationService.sendMediaNotificationEmail(azureAccount.getEmail(), TEST, false))
            .thenReturn(TRUE);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, TRUE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
            + CREATED_ACCOUNTS_KEY);
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.CREATED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, azureAccount.getAzureAccountId(), AZURE_ACCOUNT_ERROR);
        assertEquals(0, createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "Map should have no errored accounts"
        );

        verify(azureUserService).createUser(azureAccount, TRUE);
    }

    @Test
    void testAccountCreatedAlreadyExistsNoEmail() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = FULL_NAME;

        azureAccount.setRole(Roles.VERIFIED);

        when(azureUserService.getUser(EMAIL)).thenReturn(azUser);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.ERRORED_ACCOUNTS), SHOULD_CONTAIN
            + ERRORED_ACCOUNTS_KEY);
    }

    @Test
    void testAccountCreatedAlreadyExistsWithEmail() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = FULL_NAME;

        azureAccount.setRole(Roles.VERIFIED);

        when(azureUserService.getUser(EMAIL)).thenReturn(azUser);
        when(publicationService.sendNotificationEmailForDuplicateMediaAccount(any(), any())).thenReturn(TRUE);
        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
            + ERRORED_ACCOUNTS_KEY);
        assertFalse(createdAccounts.containsValue(CreationEnum.CREATED_ACCOUNTS), "Should not contain "
            + "CREATED_ACCOUNTS value");
    }

    @Test
    void testAccountCreatedAlreadyExistsWithNotGivenNameEmail() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = "";

        azureAccount.setRole(Roles.VERIFIED);

        when(azureUserService.getUser(EMAIL)).thenReturn(azUser);
        when(publicationService.sendNotificationEmailForDuplicateMediaAccount(any(), any())).thenReturn(TRUE);
        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);
        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
            + ERRORED_ACCOUNTS_KEY);
        assertFalse(createdAccounts.containsValue(CreationEnum.CREATED_ACCOUNTS), "Should not contain "
            + "CREATED_ACCOUNTS value");
    }

    @Test
    void testAccountCreatedAlreadyExistsWithNotVerifiedEmail() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = FULL_NAME;

        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        when(azureUserService.getUser(EMAIL)).thenReturn(azUser);
        when(publicationService.sendNotificationEmailForDuplicateMediaAccount(any(), any())).thenReturn(TRUE);
        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);
        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
            + ERRORED_ACCOUNTS_KEY);
        assertFalse(createdAccounts.containsValue(CreationEnum.CREATED_ACCOUNTS), "Should not contain "
            + "CREATED_ACCOUNTS value");
    }

    @Test
    void testAccountCreatedGetUserException() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.getUser(any()))
            .thenThrow(new AzureCustomException("Error when checking account into Azure."));

        AzureCustomException azureCustomException = assertThrows(AzureCustomException.class, () -> {
            azureUserService.getUser(any());
        });

        azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertEquals(
            "Error when checking account into Azure.",
            azureCustomException.getMessage(),
            "Error message should be present when failing to communicate with the AD service"
        );
    }

    @Test
    void testAccountNotCreated() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.getUser(EMAIL)).thenReturn(null);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenThrow(new AzureCustomException(ERROR_MESSAGE));

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

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
    void testAccountNotCreatedEmailNotSentWhenSystemAdmin() throws AzureCustomException {
        azureAccount.setRole(Roles.SYSTEM_ADMIN);
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.getUser(EMAIL)).thenReturn(null);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.ERRORED_ACCOUNTS), ERRORED_ACCOUNTS_VALIDATION_MESSAGE);
        List<? extends AzureAccount> accounts = createdAccounts.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals("Account has been successfully created, however email has failed to send.",
                     ((ErroredAzureAccount) accounts.get(0)).getErrorMessages().get(0),
                     "Account should have error message set when failed"
        );

        //Currently the existing process puts errored emails into both created + errored, which is why the count here
        //is 1
        assertEquals(1, createdAccounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Map should have no created accounts"
        );
    }

    @Test
    void testValidationCreatesAnErroredAccount() {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of(constraintViolation));

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

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
    void testAccountSoftErrored() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);

        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn(FALSE);

        Map<CreationEnum, List<? extends AzureAccount>> erroredAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);

        List<? extends AzureAccount> accounts = erroredAccounts.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, azureAccount.getAzureAccountId(), AZURE_ACCOUNT_ERROR);
    }

    @Test
    void creationOfMultipleAccounts() throws AzureCustomException {
        AzureAccount erroredAzureAccount = new AzureAccount();
        erroredAzureAccount.setEmail(INVALID_EMAIL);

        doReturn(Set.of()).when(validator).validate(argThat(sub -> ((AzureAccount) sub)
            .getEmail().equals(azureAccount.getEmail())));

        doReturn(Set.of(constraintViolation)).when(validator).validate(argThat(sub -> ((AzureAccount) sub)
            .getEmail().equals(erroredAzureAccount.getEmail())));

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail())),
                                         anyBoolean())).thenReturn(expectedUser);

        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn(TRUE);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            azureAccountService.addAzureAccounts(List.of(azureAccount, erroredAzureAccount), ISSUER_ID, FALSE, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
            + CREATED_ACCOUNTS_KEY);
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
    void testAzureAdminAccountFailedDoesntTriggerEmail() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());
        when(azureUserService.createUser(azureAccount, false)).thenThrow(new AzureCustomException(TEST));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountService.class)) {
            azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE, FALSE);
            assertEquals(0, logCaptor.getInfoLogs().size(),
                         "Should not log if failed creating account"
            );
        }
    }

    @Test
    void testRetrieveUser() throws AzureCustomException {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);
        user.setEmail(EMAIL);

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = FULL_NAME;
        azUser.displayName = FULL_NAME;
        azUser.surname = SURNAME;

        when(azureUserService.getUser(EMAIL)).thenReturn(azUser);
        when(userRepository.findByProvenanceUserIdAndUserProvenance(userId.toString(), UserProvenances.PI_AAD))
            .thenReturn(Optional.of(user));

        AzureAccount returnedUser = azureAccountService.retrieveAzureAccount(userId.toString());
        assertEquals(azUser.displayName, returnedUser.getDisplayName(), RETURN_USER_ERROR);
    }

    @Test
    void testRetrieveUserNotFound() {
        String userId = UUID.randomUUID().toString();
        when(userRepository.findByProvenanceUserIdAndUserProvenance(userId, UserProvenances.PI_AAD))
            .thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class, () ->
            azureAccountService.retrieveAzureAccount(userId));

        assertEquals(
            "User with supplied provenanceUserId: " + userId + " could not be found",
            exception.getMessage(),
            "Error message does not match"
        );
    }
}
