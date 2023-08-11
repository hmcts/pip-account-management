package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import nl.altindag.log.LogCaptor;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.ForbiddenRoleUpdateException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserWithProvenanceNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
class AccountServiceTest {
    @Mock
    private AzureUserService azureUserService;

    @Mock
    private AccountFilteringService accountFilteringService;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SensitivityService sensitivityService;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    AzureAccountService azureAccountService;

    @InjectMocks
    private AccountService accountService;

    private static final String FULL_NAME = "Full name";
    private static final String ISSUER_ID = "abcdef";
    private static final String EMAIL = "test@hmcts.net";
    private static final String PASSWORD = "Password123!";
    private static final UUID USER_UUID = UUID.randomUUID();
    private static final String INVALID_EMAIL = "ab.com";
    private static final String EMAIL_PREFIX = "TEST_PIP_1234_";
    private static final String ID = "1234";
    private static final String VALIDATION_MESSAGE = "Validation Message";
    private static final String TEST = "Test";
    private static final String MESSAGES_MATCH = "Messages should match";
    private static final String FORENAME = "Firstname";
    private static final String SURNAME = "Surname";
    private static final String SUBSCRIPTIONS_DELETED = "subscriptions deleted";

    private static final String USER_NOT_FOUND_EXCEPTION_MESSAGE =
        "The exception when a user has not been found has been thrown";
    private static final UUID VALID_USER_ID = UUID.randomUUID();
    private static final UUID VALID_USER_ID_IDAM = UUID.randomUUID();

    private static final PiUser PI_USER = new PiUser();
    private static final PiUser PI_USER_IDAM = new PiUser();
    private static final AzureAccount AZURE_ACCOUNT = new AzureAccount();
    private static final User EXPECTED_USER = new User();

    private static final String RETURN_USER_ERROR = "Returned user does not match expected user";

    @BeforeEach
    void setup() {
        PI_USER.setUserId(VALID_USER_ID);
        PI_USER.setUserProvenance(UserProvenances.PI_AAD);
        PI_USER.setProvenanceUserId(ID);
        PI_USER.setEmail(EMAIL);

        PI_USER_IDAM.setUserId(VALID_USER_ID_IDAM);
        PI_USER_IDAM.setUserProvenance(UserProvenances.CFT_IDAM);
        PI_USER_IDAM.setProvenanceUserId(ID);

        AZURE_ACCOUNT.setEmail(EMAIL);
        AZURE_ACCOUNT.setRole(Roles.INTERNAL_ADMIN_CTSC);

        EXPECTED_USER.givenName = TEST;
        EXPECTED_USER.id = ID;
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
        UserWithProvenanceNotFoundException ex = assertThrows(UserWithProvenanceNotFoundException.class, () ->
            accountService.findUserByProvenanceId(UserProvenances.CRIME_IDAM, TEST));
        assertEquals("No user found with provenance user ID: Test", ex.getMessage(), MESSAGES_MATCH);
    }

    @Test
    void testIsUserAuthorisedForPublicationReturnsTrue() {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(PI_USER));
        when(sensitivityService.checkAuthorisation(PI_USER, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC))
            .thenReturn(true);

        assertTrue(
            accountService.isUserAuthorisedForPublication(VALID_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC),
            "User from PI_AAD should return true for allowed list type"
        );
    }

    @Test
    void testIsUserAuthorisedForPublicationReturnsFalse() {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(PI_USER));
        when(sensitivityService.checkAuthorisation(PI_USER, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC))
            .thenReturn(false);

        assertFalse(
            accountService.isUserAuthorisedForPublication(VALID_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC),
            "User from PI_AAD should return true for allowed list type"
        );
    }

    @Test
    void testIsUserAuthorisedForPublicationReturnsException() {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.empty());
        UserNotFoundException ex = assertThrows(UserNotFoundException.class, () ->
            accountService.isUserAuthorisedForPublication(VALID_USER_ID, ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC));

        assertTrue(ex.getMessage().contains("No user found with the userId"), MESSAGES_MATCH);
    }

    @Test
    void testFindUserEmailsByIds() {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(PI_USER));

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
    void testDeleteAadAccount() throws AzureCustomException {
        when(userRepository.findByUserId(USER_UUID)).thenReturn(Optional.of(PI_USER));

        doNothing().when(userRepository).delete(PI_USER);
        when(azureUserService.deleteUser(PI_USER.getProvenanceUserId()))
            .thenReturn(EXPECTED_USER);
        when(subscriptionService.sendSubscriptionDeletionRequest(VALID_USER_ID.toString()))
            .thenReturn(SUBSCRIPTIONS_DELETED);

        accountService.deleteAccount(USER_UUID);

        verify(azureUserService, times(1)).deleteUser(PI_USER.getProvenanceUserId());
        verify(subscriptionService, times(1))
            .sendSubscriptionDeletionRequest(VALID_USER_ID.toString());
        verify(userRepository, times(1)).delete(PI_USER);
    }

    @Test
    void testDeleteIdamAccount() {
        when(userRepository.findByUserId(VALID_USER_ID_IDAM)).thenReturn(Optional.of(PI_USER_IDAM));

        doNothing().when(userRepository).delete(PI_USER_IDAM);
        when(subscriptionService.sendSubscriptionDeletionRequest(VALID_USER_ID_IDAM.toString()))
            .thenReturn(SUBSCRIPTIONS_DELETED);

        accountService.deleteAccount(VALID_USER_ID_IDAM);

        verifyNoInteractions(azureUserService);
        verify(subscriptionService).sendSubscriptionDeletionRequest(VALID_USER_ID_IDAM.toString());
        verify(userRepository).delete(PI_USER_IDAM);
    }

    @Test
    void testDeleteAccountNotFound() {
        try {
            Object result = accountService.deleteAccount(UUID.randomUUID());
            assertThrows(NotFoundException.class, () ->
                result.toString(), "Expected NotFoundException to be thrown");
        } catch (NotFoundException e) {
            assertTrue(e.getMessage()
                           .contains("User with supplied ID could not be found"),
                       "Not found error missing");
        }
    }

    @Test
    void testDeleteAccountThrows() throws AzureCustomException {
        when(userRepository.findByUserId(VALID_USER_ID)).thenReturn(Optional.of(PI_USER));

        doNothing().when(userRepository).delete(PI_USER);
        when(azureUserService.deleteUser(PI_USER.getProvenanceUserId()))
            .thenThrow(new AzureCustomException(TEST));
        when(subscriptionService.sendSubscriptionDeletionRequest(VALID_USER_ID.toString()))
            .thenReturn(SUBSCRIPTIONS_DELETED);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountService.class)) {
            accountService.deleteAccount(VALID_USER_ID);
            assertEquals(1, logCaptor.getErrorLogs().size(),
                         "No logs were thrown");
        }
    }

    @Test
    void testDeleteAllAccountsWithEmailPrefix() throws AzureCustomException {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();

        PiUser user1 = new PiUser();
        user1.setUserId(userId1);
        user1.setUserProvenance(UserProvenances.PI_AAD);

        PiUser user2 = new PiUser();
        user2.setUserId(userId2);
        user2.setUserProvenance(UserProvenances.PI_AAD);

        when(accountFilteringService.findAllAccountsExceptThirdParty(
            PageRequest.of(0, 25), EMAIL_PREFIX, "", Collections.emptyList(), Collections.emptyList(), "")
        ).thenReturn(new PageImpl<>(List.of(user1, user2)));

        when(userRepository.findByUserId(userId1)).thenReturn(Optional.of(user1));
        when(userRepository.findByUserId(userId2)).thenReturn(Optional.of(user2));

        assertThat(accountService.deleteAllAccountsWithEmailPrefix(EMAIL_PREFIX))
            .as("Account deleted message does not match")
            .isEqualTo("2 account(s) deleted with email starting with " + EMAIL_PREFIX);

        verify(azureUserService, times(2)).deleteUser(any());
        verify(subscriptionService, times(2)).sendSubscriptionDeletionRequest(any());
        verify(userRepository, times(2)).delete(any());
    }

    @Test
    void testDeleteAllAccountsWithEmailPrefixWhenAccountNoFound() throws AzureCustomException {
        when(accountFilteringService.findAllAccountsExceptThirdParty(
            PageRequest.of(0, 25), EMAIL_PREFIX, "", Collections.emptyList(), Collections.emptyList(), "")
        ).thenReturn(Page.empty());

        assertThat(accountService.deleteAllAccountsWithEmailPrefix(EMAIL_PREFIX))
            .as("Account deleted message does not match")
            .isEqualTo("0 account(s) deleted with email starting with " + EMAIL_PREFIX);

        verify(userRepository, never()).findByUserId(any());
        verify(azureUserService, never()).deleteUser(any());
        verify(subscriptionService, never()).sendSubscriptionDeletionRequest(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    void testUpdateAccountSuccessful() {
        Map<String, String> updateParameters = Map.of(
            "lastVerifiedDate", "2022-08-14T20:21:10.912Z",
            "lastSignedInDate", "2022-08-14T20:21:20.912Z"
        );

        when(userRepository.findByProvenanceUserIdAndUserProvenance(ID, UserProvenances.PI_AAD))
            .thenReturn(Optional.of(PI_USER));

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
            .thenReturn(Optional.of(PI_USER));

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
            .thenReturn(Optional.of(PI_USER));

        IllegalArgumentException illegalArgumentException = assertThrows(
            IllegalArgumentException.class, () -> accountService.updateAccount(UserProvenances.PI_AAD, ID,
                                                                               updateParameters),
            "Expected IllegalArgumentException to be thrown");

        assertTrue(illegalArgumentException.getMessage()
                       .contains("Date time value '2022-08-14' not in expected format"),
                   "Illegal argument error mismatch");
    }

    @Test
    void testGetUserById() {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        PiUser returnedUser = accountService.getUserById(userId);
        assertEquals(user, returnedUser, RETURN_USER_ERROR);
    }

    @Test
    void testGetUserByIdNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            accountService.getUserById(userId);
        }, USER_NOT_FOUND_EXCEPTION_MESSAGE);

        assertTrue(notFoundException.getMessage().contains(userId.toString()),
                   "Exception message thrown does not contain the user ID");
    }

    @Test
    void testUpdateUserAccountRoleWhenIdIsNull() throws AzureCustomException {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setProvenanceUserId(ID);

        User azUser = new User();
        azUser.id = ID;
        azUser.givenName = FULL_NAME;

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(azureUserService.updateUserRole(ID, SYSTEM_ADMIN.toString())).thenReturn(azUser);

        String response = accountService.updateAccountRole(null, userId, SYSTEM_ADMIN);
        assertEquals(String.format("User with ID %s has been updated to a SYSTEM_ADMIN", userId),
                     response, RETURN_USER_ERROR);
    }

    @Test
    void testExceptionIsThrownWhenAdminAndUserIdMatches() {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setProvenanceUserId(ID);

        ForbiddenRoleUpdateException forbiddenRoleUpdateException =
            assertThrows(ForbiddenRoleUpdateException.class, () ->
            accountService.updateAccountRole(userId, userId, SYSTEM_ADMIN));

        assertEquals(String.format("User with id %s is unable to update user ID %s", userId, userId),
                     forbiddenRoleUpdateException.getMessage(),
                     "Exception message does not match expected message");
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
        when(azureUserService.updateUserRole(ID, SYSTEM_ADMIN.toString())).thenReturn(azUser);

        String response = accountService.updateAccountRole(UUID.randomUUID(), userId, SYSTEM_ADMIN);
        assertEquals(String.format("User with ID %s has been updated to a SYSTEM_ADMIN", userId),
                    response, RETURN_USER_ERROR);
    }

    @Test
    void testUpdateUserAccountRoleCftIdam() {
        UUID userId = UUID.randomUUID();

        PiUser user = new PiUser();
        user.setUserId(userId);
        user.setUserProvenance(UserProvenances.CFT_IDAM);
        user.setProvenanceUserId(ID);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        String response = accountService.updateAccountRole(UUID.randomUUID(), userId, SYSTEM_ADMIN);
        assertEquals(String.format("User with ID %s has been updated to a SYSTEM_ADMIN", userId),
                     response, RETURN_USER_ERROR);
    }

    @Test
    void testUpdateUserAccountRoleNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            accountService.updateAccountRole(UUID.randomUUID(), userId, SYSTEM_ADMIN);
        }, USER_NOT_FOUND_EXCEPTION_MESSAGE);

        assertTrue(notFoundException.getMessage().contains(userId.toString()),
                   "Exception message thrown does not contain the user ID");
    }

    @Test
    void testUpdateUserAccountRoleAzureException() throws AzureCustomException {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(PI_USER));

        when(azureUserService.updateUserRole(any(), any()))
            .thenThrow(new AzureCustomException(TEST));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountService.class)) {
            accountService.updateAccountRole(UUID.randomUUID(), userId, SYSTEM_ADMIN);
            assertEquals(1, logCaptor.getErrorLogs().size(),
                         "Should not log if failed creating account"
            );
        }
    }

    @Test
    void shouldNotCreateSystemAdminAccountViaPiService() {
        PI_USER.setRoles(SYSTEM_ADMIN);
        Map<CreationEnum, List<?>> returnedUsers = accountService.addUsers(List.of(PI_USER), ISSUER_ID);
        assertEquals(0, returnedUsers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "Number of successful accounts does not match");
        assertEquals(1, returnedUsers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "Number of errored accounts does not match");

        ErroredPiUser erroredPiUser = (ErroredPiUser) returnedUsers.get(CreationEnum.ERRORED_ACCOUNTS).get(0);
        assertEquals("System admins must be created via the /account/add/system-admin endpoint",
                     erroredPiUser.getErrorMessages().get(0), "Error message is not correct");
    }

    @Test
    void testRetrieveUserNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        NotFoundException notFoundException = assertThrows(NotFoundException.class, () -> {
            accountService.getUserById(userId);
        }, USER_NOT_FOUND_EXCEPTION_MESSAGE);

        assertTrue(notFoundException.getMessage().contains(userId.toString()),
                   "Exception message thrown does not contain the user ID");
    }

    @Test
    void testAddUserWithSuppliedPasswordSuccess() {
        AzureAccount azureAccount = new AzureAccount(ID, EMAIL, PASSWORD, FORENAME, SURNAME,
                                              Roles.VERIFIED, null);
        Map<CreationEnum, List<? extends AzureAccount>> returnedAzureAccount = Map.of(
            CreationEnum.CREATED_ACCOUNTS,
            List.of(azureAccount),
            CreationEnum.ERRORED_ACCOUNTS,
            Collections.emptyList()
        );
        when(azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, false, true))
            .thenReturn(returnedAzureAccount);

        UUID createdUserId = UUID.randomUUID();
        PiUser createdUser = new PiUser(createdUserId, UserProvenances.PI_AAD, ID, EMAIL, Roles.VERIFIED,
                                 FORENAME, SURNAME, null, null, null);
        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(userRepository.save(any())).thenReturn(createdUser);

        Pair<CreationEnum, Object> result = accountService.addUserWithSuppliedPassword(azureAccount, ISSUER_ID);

        assertThat(result.getKey())
            .as(RETURN_USER_ERROR)
            .isEqualTo(CreationEnum.CREATED_ACCOUNTS);

        PiUser returnedUser = (PiUser) result.getValue();
        assertThat(returnedUser.getEmail())
            .as("Returned user email does not match")
            .isEqualTo(EMAIL);

        assertThat(returnedUser.getUserId())
            .as("Returned user ID does not match")
            .isEqualTo(createdUserId);

        assertThat(returnedUser.getProvenanceUserId())
            .as("Returned provenance user ID does not match")
            .isEqualTo(ID);
    }

    @Test
    void testAddUserWithSuppliedPasswordIfAddAzureAccountErrored() {
        AzureAccount azureAccount = new AzureAccount(ID, EMAIL, PASSWORD, FORENAME, SURNAME,
                                                     Roles.VERIFIED, null);
        ErroredAzureAccount erroredAzureAccount = new ErroredAzureAccount(azureAccount);
        erroredAzureAccount.setErrorMessages(List.of(VALIDATION_MESSAGE));

        Map<CreationEnum, List<? extends AzureAccount>> returnedAzureAccount = Map.of(
            CreationEnum.CREATED_ACCOUNTS,
            Collections.emptyList(),
            CreationEnum.ERRORED_ACCOUNTS,
            List.of(erroredAzureAccount)
        );
        when(azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, false, true))
            .thenReturn(returnedAzureAccount);

        Pair<CreationEnum, Object> result = accountService.addUserWithSuppliedPassword(azureAccount, ISSUER_ID);
        assertThat(result.getKey())
            .as(RETURN_USER_ERROR)
            .isEqualTo(CreationEnum.ERRORED_ACCOUNTS);

        assertThat(result.getValue())
            .as(RETURN_USER_ERROR)
            .isEqualTo(List.of(VALIDATION_MESSAGE).toString());

        verifyNoInteractions(userRepository);
    }

    @Test
    void testAddUserWithSuppliedPasswordIfAddPiUserErrored() {
        AzureAccount azureAccount = new AzureAccount(ID, EMAIL, PASSWORD, FORENAME, SURNAME,
                                                     Roles.VERIFIED, null);
        Map<CreationEnum, List<? extends AzureAccount>> returnedAzureAccount = Map.of(
            CreationEnum.CREATED_ACCOUNTS,
            List.of(azureAccount)
        );
        when(azureAccountService.addAzureAccounts(List.of(azureAccount), ISSUER_ID, false, true))
            .thenReturn(returnedAzureAccount);
        when(constraintViolation.getMessage()).thenReturn(VALIDATION_MESSAGE);
        doReturn(Set.of(constraintViolation)).when(validator).validate(any());

        Pair<CreationEnum, Object> result = accountService.addUserWithSuppliedPassword(azureAccount, ISSUER_ID);
        assertThat(result.getKey())
            .as(RETURN_USER_ERROR)
            .isEqualTo(CreationEnum.ERRORED_ACCOUNTS);

        assertThat(result.getValue())
            .as(RETURN_USER_ERROR)
            .isEqualTo(List.of(VALIDATION_MESSAGE).toString());

        verifyNoInteractions(userRepository);
    }

    @Test
    void testAddUserWithSuppliedPasswordIfPasswordMissing() {
        AzureAccount azureAccount = new AzureAccount(ID, EMAIL, null, FORENAME, SURNAME,
                                                     Roles.VERIFIED, null);

        Pair<CreationEnum, Object> result = accountService.addUserWithSuppliedPassword(azureAccount, ISSUER_ID);
        assertThat(result.getKey())
            .as(RETURN_USER_ERROR)
            .isEqualTo(CreationEnum.ERRORED_ACCOUNTS);

        assertThat(result.getValue())
            .as(RETURN_USER_ERROR)
            .isEqualTo("Password must not be blank");

        verifyNoInteractions(azureAccountService);
        verifyNoInteractions(validator);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testAddUserWithSuppliedPasswordIfPasswordEmpty() {
        AzureAccount azureAccount = new AzureAccount(ID, EMAIL, "", FORENAME, SURNAME,
                                                     Roles.VERIFIED, null);

        Pair<CreationEnum, Object> result = accountService.addUserWithSuppliedPassword(azureAccount, ISSUER_ID);
        assertThat(result.getKey())
            .as(RETURN_USER_ERROR)
            .isEqualTo(CreationEnum.ERRORED_ACCOUNTS);

        assertThat(result.getValue())
            .as(RETURN_USER_ERROR)
            .isEqualTo("Password must not be blank");

        verifyNoInteractions(azureAccountService);
        verifyNoInteractions(validator);
        verifyNoInteractions(userRepository);
    }
}
