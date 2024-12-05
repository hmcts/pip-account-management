package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SystemAdminAccountException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAdminB2CAccountServiceTest {

    @Mock
    private AzureUserService azureUserService;

    @Mock
    private PublicationService publicationService;

    @Mock
    private AccountService accountService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @Mock
    private Path path;

    private SystemAdminB2CAccountService systemAdminAccountService;

    private static final UUID ID = UUID.randomUUID();
    private static final String EMAIL = "test@email.com";
    private static final String FORENAME = "Test";
    private static final String SURNAME = "Surname";
    private static final SystemAdminAccount SYSTEM_ADMIN_ACCOUNT = new SystemAdminAccount(EMAIL, FORENAME, SURNAME);
    private static final SystemAdminAccount ERRORED_SYSTEM_ADMIN_ACCOUNT = new SystemAdminAccount("abcd", FORENAME,
                                                                                               SURNAME);
    private static final String USER_MESSAGE = "returned user did not match expected";

    private final User expectedUser = new User();
    private final PiUser expectedPiUser = new PiUser();
    private final PiUser ssoUser = new PiUser();
    private final PiUser systemAdminUser = new PiUser();

    @BeforeEach
    void setup() {
        expectedUser.setGivenName(FORENAME);
        expectedUser.setId(ID.toString());
        expectedUser.setSurname(SURNAME);

        expectedPiUser.setUserId(UUID.randomUUID());
        expectedPiUser.setEmail(EMAIL);
        expectedPiUser.setProvenanceUserId(ID.toString());
        expectedPiUser.setRoles(Roles.SYSTEM_ADMIN);
        expectedPiUser.setUserProvenance(UserProvenances.PI_AAD);

        ssoUser.setUserId(UUID.randomUUID());
        ssoUser.setEmail(EMAIL);
        ssoUser.setProvenanceUserId(UUID.randomUUID().toString());
        ssoUser.setRoles(Roles.SYSTEM_ADMIN);
        ssoUser.setUserProvenance(UserProvenances.SSO);

        systemAdminUser.setRoles(Roles.SYSTEM_ADMIN);
        systemAdminUser.setUserId(ID);
        systemAdminUser.setEmail(EMAIL);

        systemAdminAccountService = new SystemAdminB2CAccountService(validator, azureUserService, userRepository,
                                                                     publicationService, 4,
                                                                     accountService);

    }

    @Test
    void testAddSystemAdminAccount() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        when(azureUserService.createUser(argThat(user -> EMAIL.equals(user.getEmail())), anyBoolean()))
            .thenReturn(expectedUser);
        when(userRepository.save(any())).thenReturn(expectedPiUser);
        when(publicationService.sendNotificationEmail(EMAIL, FORENAME, SURNAME)).thenReturn(Boolean.TRUE);

        doNothing().when(publicationService)
            .sendSystemAdminAccountAction(argThat(arg -> EMAIL.equals(arg.getRequesterEmail())));

        when(accountService.getUserById(ID)).thenReturn(systemAdminUser);
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Set.of());
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser));

        PiUser returnedUser = systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID.toString());

        assertEquals(expectedPiUser, returnedUser, USER_MESSAGE);
    }

    @Test
    void testAddSystemAdminAccountThrowsException() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        when(accountService.getUserById(ID)).thenReturn(systemAdminUser);
        when(azureUserService.createUser(argThat(user -> EMAIL.equals(user.getEmail())), anyBoolean()))
            .thenThrow(new AzureCustomException("Test error"));

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID.toString()));


        assertEquals("Test error",
                     systemAdminAccountException.getErroredSystemAdminAccount().getErrorMessages().get(0),
                     "Error message not as expected");
    }

    @Test
    void testConstraintViolationException() {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        when(accountService.getUserById(ID)).thenReturn(systemAdminUser);
        when(validator.validate(any())).thenReturn(Set.of(constraintViolation));
        when(constraintViolation.getMessage()).thenReturn("This is a message");
        when(constraintViolation.getPropertyPath()).thenReturn(path);

        doNothing().when(publicationService)
            .sendSystemAdminAccountAction(argThat(arg -> EMAIL.equals(arg.getRequesterEmail())));

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(ERRORED_SYSTEM_ADMIN_ACCOUNT, ID.toString()));

        assertNotEquals(0, systemAdminAccountException.getErroredSystemAdminAccount().getErrorMessages().size(),
                   "Constraint violation error messages not displayed");
    }

    @Test
    void testAddSystemAdminAccountNotExists() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        expectedPiUser.setRoles(Roles.VERIFIED);
        when(azureUserService.createUser(argThat(user -> EMAIL.equals(user.getEmail())), anyBoolean()))
            .thenReturn(expectedUser);
        when(userRepository.save(any())).thenReturn(expectedPiUser);
        when(publicationService.sendNotificationEmail(EMAIL, FORENAME, SURNAME)).thenReturn(Boolean.FALSE);
        when(accountService.getUserById(ID)).thenReturn(new PiUser());
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Set.of());
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser));
        doNothing().when(publicationService)
            .sendSystemAdminAccountAction(argThat(arg -> arg.getRequesterEmail() == null));

        PiUser returnedUser = systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID.toString());

        assertEquals(expectedPiUser, returnedUser, USER_MESSAGE);
    }

    @Test
    void testUserAlreadyExists() {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.PI_AAD))
            .thenReturn(Optional.of(expectedPiUser));
        doNothing().when(publicationService)
            .sendSystemAdminAccountAction(argThat(arg -> EMAIL.equals(arg.getRequesterEmail())));
        when(accountService.getUserById(ID))
            .thenReturn(systemAdminUser);

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID.toString()));

        assertTrue(systemAdminAccountException.getErroredSystemAdminAccount().isDuplicate(), "Duplicate account flag "
            + "not set");
    }

    @Test
    void testAboveMaxAllowsUsersWithAllAadUsers() {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.PI_AAD))
            .thenReturn(Optional.empty());
        when(accountService.getUserById(ID))
            .thenReturn(systemAdminUser);
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser, expectedPiUser,
                                                                                expectedPiUser, expectedPiUser));

        doNothing().when(publicationService)
            .sendSystemAdminAccountAction(argThat(arg -> EMAIL.equals(arg.getRequesterEmail())));

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID.toString()));

        assertTrue(systemAdminAccountException.getErroredSystemAdminAccount().isAboveMaxSystemAdmin(), "Max system "
            + "admin flag not set");
    }

    @Test
    void testAboveMaxAllowsUsersNotIncludingSsoUser() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.PI_AAD))
            .thenReturn(Optional.empty());
        when(accountService.getUserById(ID))
            .thenReturn(systemAdminUser);
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser, expectedPiUser,
                                                                                expectedPiUser, ssoUser));
        doNothing().when(publicationService)
            .sendSystemAdminAccountAction(argThat(arg -> EMAIL.equals(arg.getRequesterEmail())));

        when(azureUserService.createUser(argThat(user -> EMAIL.equals(user.getEmail())), anyBoolean()))
            .thenReturn(expectedUser);
        when(userRepository.save(any())).thenReturn(expectedPiUser);

        PiUser returnedUser = systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID.toString());

        assertEquals(expectedPiUser, returnedUser, USER_MESSAGE);
    }

    @Test
    void testHandleNewSystemAdminAccountAction() {
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser, expectedPiUser));

        ArgumentCaptor<CreateSystemAdminAction> systemAdminAccountArgumentCaptor =
            ArgumentCaptor.forClass(CreateSystemAdminAction.class);

        doNothing().when(publicationService).sendSystemAdminAccountAction(systemAdminAccountArgumentCaptor.capture());

        systemAdminAccountService.handleNewSystemAdminAccountAction(SYSTEM_ADMIN_ACCOUNT,
                                                                    ID.toString(),
                                                                    ActionResult.ATTEMPTED,
                                                                    EMAIL);

        CreateSystemAdminAction createSystemAdminAction = systemAdminAccountArgumentCaptor.getValue();

        assertEquals(EMAIL, createSystemAdminAction.getAccountEmail(), "Unknown email retrieved");
        assertEquals(EMAIL, createSystemAdminAction.getRequesterEmail(), "Unknown requester name retrieved");
        assertEquals(List.of(EMAIL, EMAIL), createSystemAdminAction.getEmailList(), "Unknown email list retrieved");
        assertEquals(ActionResult.ATTEMPTED, createSystemAdminAction.getActionResult(),
                     "Action result not as expected");
    }
}
