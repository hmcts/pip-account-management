package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_NON_RESTRICTED_ADMIN_ROLES;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.LawOfDemeter"})
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

    @Mock
    private SensitivityService sensitivityService;

    @Mock
    private AccountModelMapperService accountModelMapperService;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private AccountService accountService;

    private static final String FULL_NAME = "Full name";
    private static final String ISSUER_ID = "abcdef";
    private static final String EMAIL = "a@b.com";
    private static final UUID USER_UUID = UUID.randomUUID();
    private static final String INVALID_EMAIL = "ab.com";
    private static final String ID = "1234";
    private static final String EMAIL_PATH = "Email";
    private static final String ERROR_MESSAGE = "An error has been found";
    private static final String VALIDATION_MESSAGE = "Validation Message";
    private static final String EMAIL_VALIDATION_MESSAGE = "AzureAccount should have expected email";
    private static final String ERRORED_ACCOUNTS_VALIDATION_MESSAGE = "Should contain ERRORED_ACCOUNTS key";
    private static final String TEST = "Test";
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final boolean FALSE = false;
    private static final boolean TRUE = true;
    private static final String SHOULD_CONTAIN = "Should contain ";
    private static final String FORENAME = "Firstname";
    private static final String SURNAME = "Surname";
    private static final String SUBSCRIPTIONS_DELETED = "subscriptions deleted";
    public static final List<String> EXAMPLE_CSV = List.of(
        "2fe899ff-96ed-435a-bcad-1411bbe96d2a,string,CFT_IDAM,INTERNAL_ADMIN_CTSC");

    private static final UUID VALID_USER_ID = UUID.randomUUID();
    private static final UUID VALID_USER_ID_IDAM = UUID.randomUUID();

    private final PiUser piUser = new PiUser();
    private final PiUser piUserIdam = new PiUser();
    private final MediaApplication mediaAndLegalApplication = new MediaApplication();
    private AzureAccount azureAccount;
    private User expectedUser;

    @BeforeEach
    void setup() {
        piUser.setUserId(VALID_USER_ID);
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setProvenanceUserId(ID);
        piUser.setEmail(EMAIL);

        piUserIdam.setUserId(VALID_USER_ID_IDAM);
        piUserIdam.setUserProvenance(UserProvenances.CFT_IDAM);
        piUserIdam.setProvenanceUserId(ID);

        mediaAndLegalApplication.setEmail(EMAIL);
        mediaAndLegalApplication.setFullName(FULL_NAME);

        azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        expectedUser = new User();
        expectedUser.givenName = TEST;
        expectedUser.id = ID;
        expectedUser.givenName = "Test User";

        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(piUser));
        when(userRepository.findByUserId(VALID_USER_ID_IDAM)).thenReturn(Optional.of(piUserIdam));
        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);
        when(constraintViolation.getPropertyPath()).thenReturn(path);
        when(path.toString()).thenReturn(EMAIL_PATH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"INTERNAL_ADMIN_CTSC", "SYSTEM_ADMIN"})
    void testAccountCreated(String role) throws AzureCustomException {
        azureAccount.setRole(Roles.valueOf(role));

        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        when(azureUserService.getUser(EMAIL)).thenReturn(null);

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(expectedUser);

        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn(TRUE);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
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
    void testAccountCreatedAlreadyExistsNoEmail() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = FULL_NAME;

        azureAccount.setRole(Roles.VERIFIED);

        when(azureUserService.getUser(EMAIL)).thenReturn(azUser);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.ERRORED_ACCOUNTS), SHOULD_CONTAIN
            + "ERRORED_ACCOUNTS key");
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
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
            + "ERRORED_ACCOUNTS key");
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

        accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);

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

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail()))))
            .thenThrow(new AzureCustomException(ERROR_MESSAGE));

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);

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
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);

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

        when(azureUserService.createUser(argThat(user -> user.getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(expectedUser);

        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn(FALSE);

        Map<CreationEnum, List<? extends AzureAccount>> erroredAccounts =
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);

        List<? extends AzureAccount> accounts = erroredAccounts.get(CreationEnum.ERRORED_ACCOUNTS);
        assertEquals(azureAccount.getEmail(), accounts.get(0).getEmail(), EMAIL_VALIDATION_MESSAGE);
        assertEquals(ID, azureAccount.getAzureAccountId(), "AzureAccount should have azure "
            + "object ID");
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

        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn(TRUE);

        Map<CreationEnum, List<? extends AzureAccount>> createdAccounts =
            accountService.addAzureAccounts(List.of(azureAccount, erroredAzureAccount), ISSUER_ID, FALSE);

        assertTrue(createdAccounts.containsKey(CreationEnum.CREATED_ACCOUNTS), SHOULD_CONTAIN
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
        Map<CreationEnum, List<?>> expected = new ConcurrentHashMap<>();
        PiUser user = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL, Roles.INTERNAL_ADMIN_CTSC,
                                 FORENAME, SURNAME, null, null, null);
        expected.put(CreationEnum.CREATED_ACCOUNTS, List.of(user.getUserId()));
        expected.put(CreationEnum.ERRORED_ACCOUNTS, List.of());

        when(validator.validate(user)).thenReturn(Set.of());
        when(userRepository.save(user)).thenReturn(user);

        assertEquals(expected, accountService.addUsers(List.of(user), EMAIL), "Returned maps should match");
    }

    @Test
    void testAddDuplicateUsers() {
        PiUser user1 = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL,
                                  Roles.INTERNAL_ADMIN_CTSC, FORENAME, SURNAME, null, null, null);
        PiUser user2 = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, "567", "test@test.com",

                                 Roles.INTERNAL_ADMIN_CTSC, FORENAME, SURNAME, null, null, null);
        List<PiUser> users = new ArrayList<>();
        users.add(user1);
        users.add(user2);

        List<UUID> createdAccounts = new ArrayList<>();
        createdAccounts.add(user1.getUserId());
        createdAccounts.add(user2.getUserId());

        Map<CreationEnum, List<?>> expected = new ConcurrentHashMap<>();
        expected.put(CreationEnum.CREATED_ACCOUNTS, createdAccounts);
        expected.put(CreationEnum.ERRORED_ACCOUNTS, List.of());

        when(validator.validate(user1)).thenReturn(Set.of());
        when(userRepository.save(user1)).thenReturn(user1);

        when(validator.validate(user2)).thenReturn(Set.of());
        when(userRepository.save(user2)).thenReturn(user2);

        assertEquals(expected, accountService.addUsers(users, EMAIL), "Returned maps should match");
    }

    @Test
    void testAddUsersBuildsErrored() {
        PiUser user = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, INVALID_EMAIL,
                                 Roles.INTERNAL_ADMIN_CTSC, FORENAME, SURNAME, null, null, null);
        ErroredPiUser erroredUser = new ErroredPiUser(user);
        erroredUser.setErrorMessages(List.of(VALIDATION_MESSAGE));
        Map<CreationEnum, List<?>> expected = new ConcurrentHashMap<>();
        expected.put(CreationEnum.CREATED_ACCOUNTS, List.of());
        expected.put(CreationEnum.ERRORED_ACCOUNTS, List.of(erroredUser));

        when(userRepository.save(user)).thenReturn(user);
        doReturn(Set.of(constraintViolation)).when(validator).validate(user);

        assertEquals(1, accountService.addUsers(List.of(user), EMAIL)
                       .get(CreationEnum.ERRORED_ACCOUNTS).size(), "Errored accounts not expected value");

        assertEquals(0, accountService.addUsers(List.of(user), EMAIL)
                       .get(CreationEnum.CREATED_ACCOUNTS).size(), "Created accounts not expected value");
    }

    @Test
    void testFindUserByProvenanceId() {
        PiUser user = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL, Roles.INTERNAL_ADMIN_CTSC,
                                 FORENAME, SURNAME, null, null, null);
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
        when(sensitivityService.checkAuthorisation(piUser, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC))
            .thenReturn(true);

        assertTrue(
            accountService.isUserAuthorisedForPublication(VALID_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC),
            "User from PI_AAD should return true for allowed list type"
        );
    }

    @Test
    void testIsUserAuthorisedForPublicationReturnsFalse() {
        when(sensitivityService.checkAuthorisation(piUser, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC))
            .thenReturn(false);

        assertFalse(
            accountService.isUserAuthorisedForPublication(VALID_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC),
            "User from PI_AAD should return true for allowed list type"
        );
    }


    @Test
    void testAzureAdminAccountFailedDoesntTriggerEmail() throws AzureCustomException {
        when(validator.validate(argThat(sub -> ((AzureAccount) sub).getEmail().equals(azureAccount.getEmail()))))
            .thenReturn(Set.of());
        when(azureUserService.createUser(azureAccount)).thenThrow(new AzureCustomException(TEST));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountService.class)) {
            accountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, FALSE);
            assertEquals(0, logCaptor.getInfoLogs().size(),
                         "Should not log if failed creating account"
            );
        }
    }

    @Test
    void testFindUserEmailsByIds() {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(piUser));

        List<String> userIdsList = new ArrayList<>();
        userIdsList.add(VALID_USER_ID.toString());

        Map<String, Optional<String>> expectedUserEmailMap = new ConcurrentHashMap<>();
        expectedUserEmailMap.put(VALID_USER_ID.toString(), Optional.of(EMAIL));

        assertEquals(expectedUserEmailMap, accountService.findUserEmailsByIds(userIdsList),
                     "Returned map does not match with expected map"
        );
    }

    @Test
    void testFindUserEmailsByIdsNoEmails() {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.empty());

        List<String> userIdsList = new ArrayList<>();
        userIdsList.add(VALID_USER_ID.toString());

        Map<String, Optional<String>> expectedUserEmailMap = new ConcurrentHashMap<>();
        expectedUserEmailMap.put(VALID_USER_ID.toString(), Optional.empty());

        assertEquals(expectedUserEmailMap, accountService.findUserEmailsByIds(userIdsList),
                     "Returned map does not match with expected map"
        );
    }

    @Test
    void testUploadMediaFromCsv() throws AzureCustomException, IOException {
        PiUser user1 = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL, Roles.VERIFIED, "Test", "User",
                                  null,null, null);
        PiUser user2 = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD, ID, EMAIL, Roles.VERIFIED, "Test", "User",
                                  null, null, null);

        when(validator.validate(any())).thenReturn(Set.of());
        when(azureUserService.createUser(any())).thenReturn(expectedUser);
        when(publicationService.sendNotificationEmail(any(), any(), any())).thenReturn(TRUE);
        when(accountModelMapperService.createAzureUsersFromCsv(any())).thenReturn(List.of(azureAccount, azureAccount));
        when(accountModelMapperService.createPiUsersFromAzureAccounts(List.of(azureAccount, azureAccount)))
            .thenReturn(List.of(user1, user2));
        when(userRepository.save(user1)).thenReturn(user1);
        when(userRepository.save(user2)).thenReturn(user2);

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/valid.csv")) {
            MultipartFile multipartFile = new MockMultipartFile("file", "TestFileName",
                                                                "text/plain",
                                                                IOUtils.toByteArray(inputStream)
            );

            assertEquals(2, accountService.uploadMediaFromCsv(multipartFile, EMAIL)
                .get(CreationEnum.CREATED_ACCOUNTS).size(), "Created account size should match");


        }
    }

    @Test
    void testUploadMediaFromInvalidCsv() throws IOException {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/invalidCsv.txt")) {
            MultipartFile multipartFile = new MockMultipartFile("file", "TestFileName",
                                                                "text/plain",
                                                                IOUtils.toByteArray(inputStream)
            );

            CsvParseException ex = assertThrows(CsvParseException.class, () ->
                                                    accountService.uploadMediaFromCsv(multipartFile, EMAIL),
                                                "Should throw CsvParseException"
            );
            assertTrue(ex.getMessage().contains("Failed to parse CSV File due to"), MESSAGES_MATCH);
        }
    }

    @Test
    void testDeleteAadAccount() throws AzureCustomException {
        when(userRepository.findByUserId(USER_UUID)).thenReturn(Optional.of(piUser));

        doNothing().when(userRepository).delete(piUser);
        when(azureUserService.deleteUser(piUser.getProvenanceUserId()))
            .thenReturn(expectedUser);
        when(subscriptionService.sendSubscriptionDeletionRequest(VALID_USER_ID.toString()))
            .thenReturn(SUBSCRIPTIONS_DELETED);

        accountService.deleteAccount(USER_UUID);

        verify(azureUserService, times(1)).deleteUser(piUser.getProvenanceUserId());
        verify(subscriptionService, times(1))
            .sendSubscriptionDeletionRequest(VALID_USER_ID.toString());
        verify(userRepository, times(1)).delete(piUser);
    }

    @Test
    void testDeleteIdamAccount() {
        when(userRepository.findByUserId(VALID_USER_ID_IDAM)).thenReturn(Optional.of(piUserIdam));

        doNothing().when(userRepository).delete(piUserIdam);
        when(subscriptionService.sendSubscriptionDeletionRequest(VALID_USER_ID_IDAM.toString()))
            .thenReturn(SUBSCRIPTIONS_DELETED);

        accountService.deleteAccount(VALID_USER_ID_IDAM);

        verifyNoInteractions(azureUserService);
        verify(subscriptionService).sendSubscriptionDeletionRequest(VALID_USER_ID_IDAM.toString());
        verify(userRepository).delete(piUserIdam);
    }

    @Test
    void testDeleteAccountNotFound() {
        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
            accountService.deleteAccount(UUID.randomUUID()), "Expected NotFoundException to be thrown");

        assertTrue(notFoundException.getMessage()
                       .contains("User with supplied ID could not be found"),
                   "Not found error missing");
    }

    @Test
    void testDeleteAccountThrows() throws AzureCustomException {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(piUser));

        doNothing().when(userRepository).delete(piUser);
        when(azureUserService.deleteUser(piUser.getProvenanceUserId()))
            .thenThrow(new AzureCustomException(TEST));
        when(subscriptionService.sendSubscriptionDeletionRequest(VALID_USER_ID.toString()))
            .thenReturn(SUBSCRIPTIONS_DELETED);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountService.class)) {
            accountService.deleteAccount(VALID_USER_ID);
            assertEquals(2, logCaptor.getInfoLogs().size(),
                         "No logs were thrown");
        }
    }

    @Test
    void testMiService() {
        when(userRepository.getAccManDataForMI()).thenReturn(EXAMPLE_CSV);
        String testString = accountService.getAccManDataForMiReporting();
        assertThat(testString)
            .as("Json parsing has probably failed")
            .contains("CTSC")
            .hasLineCount(2);
        String[] splitLineString = testString.split("(\r\n|\r|\n)");
        long countLine1 = splitLineString[0].chars().filter(character -> character == ',').count();
        assertThat(testString)
            .as("Header row missing")
            .contains("provenance_user_id");
        assertThat(splitLineString.length)
            .as("Data must be missing, are only headers printing?")
            .isGreaterThanOrEqualTo(2);
        assertThat(splitLineString)
            .as("Wrong comma count compared to header row!")
            .allSatisfy(
                e -> assertThat(e.chars().filter(character -> character == ',').count()).isEqualTo(countLine1));
    }

    @Test
    void testUpdateAccountSuccessful() {
        Map<String, String> updateParameters = Map.of(
            "lastVerifiedDate", "2022-08-14T20:21:10.912Z",
            "lastSignedInDate", "2022-08-14T20:21:20.912Z"
        );

        when(userRepository.findByProvenanceUserIdAndUserProvenance(ID, UserProvenances.PI_AAD))
            .thenReturn(Optional.of(piUser));

        assertEquals("Account with provenance PI_AAD and provenance id " + ID + " has been updated",
                     accountService.updateAccount(UserProvenances.PI_AAD, ID, updateParameters),
                     "Return message does not match expected");
    }

    @Test
    void testUpdateAccountUserNotFound() {
        Map<String, String> updateParameters = Map.of("lastVerifiedDate", "2022-08-14T20:21:10.912Z");

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () ->
                                                               accountService.updateAccount(UserProvenances.PI_AAD, ID,
                                                                                            updateParameters),
                                                           "Expected NotFoundException to be thrown");

        assertTrue(notFoundException.getMessage()
                       .contains("User with supplied provenance id: " + ID + " could not be found"),
                   "Not found error mismatch");
    }

    @Test
    void testUpdateAccountIllegalUpdateParameter() {
        Map<String, String> updateParameters = Map.of("nonExistentField", "value");

        when(userRepository.findByProvenanceUserIdAndUserProvenance(ID, UserProvenances.PI_AAD))
            .thenReturn(Optional.of(piUser));

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class, () -> accountService.updateAccount(UserProvenances.PI_AAD, ID,
                                                                               updateParameters),
            "Expected IllegalArgumentException to be thrown");

        assertTrue(illegalArgumentException.getMessage()
                       .contains("The field 'nonExistentField' could not be updated"),
                   "Illegal argument error mismatch");
    }

    @Test
    void testUpdateAccountUnexpectedDateTimeFormat() {
        Map<String, String> updateParameters = Map.of("lastSignedInDate", "2022-08-14");

        when(userRepository.findByProvenanceUserIdAndUserProvenance(ID, UserProvenances.PI_AAD))
            .thenReturn(Optional.of(piUser));

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class, () -> accountService.updateAccount(UserProvenances.PI_AAD, ID,
                                                                               updateParameters),
            "Expected IllegalArgumentException to be thrown");

        assertTrue(illegalArgumentException.getMessage()
                       .contains("Date time value '2022-08-14' not in expected format"),
                   "Illegal argument error mismatch");
    }

    @Test
    void testFindAllThirdPartyAccounts() {

        PiUser thirdPartyUser = new PiUser();
        thirdPartyUser.setUserId(UUID.randomUUID());
        List<PiUser> usersList = List.of(thirdPartyUser);

        when(userRepository.findAllByUserProvenance(UserProvenances.THIRD_PARTY)).thenReturn(usersList);

        List<PiUser> returnedUsers = accountService.findAllThirdPartyAccounts();

        assertEquals(usersList, returnedUsers, "Returned users does not match expected users");
    }

    @Test
    void testGetUserById() {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        PiUser returnedUser = accountService.getUserById(userId);
        assertEquals(user, returnedUser, "Returned user does not match expected user");
    }

    @Test
    void testGetUserByIdNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            accountService.getUserById(userId);
        }, "The exception when a user has not been found has been thrown");

        assertTrue(notFoundException.getMessage().contains(userId.toString()),
                   "Exception message thrown does not contain the user ID");
    }

    @Test
    void testUpdateUserAccountRolePiAad() throws AzureCustomException {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setProvenanceUserId(ID);

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = FULL_NAME;

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(azureUserService.updateUserRole(ID, Roles.SYSTEM_ADMIN.toString())).thenReturn(azUser);

        String response = accountService.updateAccountRole(userId, Roles.SYSTEM_ADMIN);
        assertEquals(String.format("User with ID %s has been updated to a SYSTEM_ADMIN", userId),
                     response, "Returned user does not match expected user");
    }

    @Test
    void testUpdateUserAccountRoleCftIdam() {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);
        user.setUserProvenance(UserProvenances.CFT_IDAM);
        user.setProvenanceUserId(ID);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        String response = accountService.updateAccountRole(userId, Roles.SYSTEM_ADMIN);
        assertEquals(String.format("User with ID %s has been updated to a SYSTEM_ADMIN", userId),
                     response, "Returned user does not match expected user");
    }

    @Test
    void testUpdateUserAccountRoleNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            accountService.updateAccountRole(userId, Roles.SYSTEM_ADMIN);
        }, "The exception when a user has not been found has been thrown");

        assertTrue(notFoundException.getMessage().contains(userId.toString()),
                   "Exception message thrown does not contain the user ID");
    }

    @Test
    void testFindAllAccountsExceptThirdPartyEmptyParams() {
        Pageable pageable = PageRequest.of(0, 25);
        List<UserProvenances> emptyUserProvenancesList = new ArrayList<>();
        List<Roles> emptyRoleList = new ArrayList<>();
        Page<PiUser> page = new PageImpl<>(List.of(piUser), pageable, List.of(piUser).size());
        when(userRepository.findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
            argThat(arg -> "%%".equals(arg)),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(page);

        Page<PiUser> response = accountService.findAllAccountsExceptThirdParty(pageable, "", "",
                                                                               emptyUserProvenancesList, emptyRoleList,
                                                                               "");

        verify(userRepository, never()).findByUserIdPageable(
            any(), any());

        assertEquals(page, response, "Returned page did not match expected");

    }

    @Test
    void testFindAllAccountsExceptThirdPartyFullParams() {
        Pageable pageable = PageRequest.of(0, 25);
        List<UserProvenances> userProvenancesList = List.of(UserProvenances.PI_AAD);
        List<Roles> roleList = List.of(Roles.VERIFIED);
        Page<PiUser> page = new PageImpl<>(List.of(piUser), pageable, List.of(piUser).size());
        when(userRepository.findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
            any(),
            any(),
            any(),
            any(),
            any()
        )).thenReturn(page);

        Page<PiUser> response = accountService.findAllAccountsExceptThirdParty(pageable, "test", ID,
                                                                               userProvenancesList, roleList, "");

        verify(userRepository, never()).findByUserIdPageable(
            any(), any());

        assertEquals(page, response, "Returned page did not match expected");

    }

    @Test
    void testFindAllAccountsExceptThirdPartyOnlyUserId() {
        Pageable pageable = PageRequest.of(0, 25);
        PiUser user = new PiUser();
        user.setUserId(UUID.randomUUID());
        List<UserProvenances> emptyUserProvenancesList = new ArrayList<>();
        List<Roles> emptyRoleList = new ArrayList<>();
        Page<PiUser> page = new PageImpl<>(List.of(user), pageable, List.of(user).size());

        when(userRepository.findByUserIdPageable(user.getUserId().toString(), pageable)).thenReturn(page);

        Page<PiUser> response = accountService.findAllAccountsExceptThirdParty(pageable, "", "",
            emptyUserProvenancesList, emptyRoleList, user.getUserId().toString());

        verify(userRepository, never())
            .findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
                any(), any(), any(), any(), any());

        assertEquals(page, response, "Returned page did not match expected");
    }

    @Test
    void testGetAdminUserByEmailAndProvenance() {
        PiUser user = new PiUser();
        user.setEmail(EMAIL);
        user.setUserProvenance(UserProvenances.PI_AAD);

        when(userRepository.findByEmailIgnoreCaseAndUserProvenanceAndRolesIn(EMAIL, UserProvenances.PI_AAD,
                                                                             ALL_NON_RESTRICTED_ADMIN_ROLES))
            .thenReturn(Optional.of(user));

        PiUser returnedUser = accountService.getAdminUserByEmailAndProvenance(EMAIL, UserProvenances.PI_AAD);
        assertEquals(user, returnedUser, "Returned user does not match expected user");
    }

    @Test
    void testGetAdminUserByEmailAndProvenanceNotFound() {
        PiUser user = new PiUser();
        user.setEmail(EMAIL);
        user.setUserProvenance(UserProvenances.PI_AAD);

        when(userRepository.findByEmailIgnoreCaseAndUserProvenanceAndRolesIn(EMAIL, UserProvenances.PI_AAD,
                                                                             ALL_NON_RESTRICTED_ADMIN_ROLES))
            .thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            accountService.getAdminUserByEmailAndProvenance(EMAIL, UserProvenances.PI_AAD);
        }, "The exception when a user has not been found has been thrown");

        assertTrue(notFoundException.getMessage().contains(EMAIL),
                   "Exception message thrown does not contain email");

        assertTrue(notFoundException.getMessage().contains(UserProvenances.PI_AAD.toString()),
                   "Exception message thrown does not contain provenance");
    }
}
