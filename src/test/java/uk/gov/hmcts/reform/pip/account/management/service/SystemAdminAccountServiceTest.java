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
class SystemAdminAccountServiceTest {

    @Mock
    private AzureUserService azureUserService;

    @Mock
    private PublicationService publicationService;

    @Mock
    private AzureAccountService azureAccountService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Validator validator;

    @Mock
    private ConstraintViolation<Object> constraintViolation;

    @Mock
    private Path path;

    private SystemAdminAccountService systemAdminAccountService;

    private static final String ID = UUID.randomUUID().toString();
    private static final String EMAIL = "test@email.com";
    private static final String FORENAME = "Test";
    private static final String SURNAME = "Surname";
    private static final SystemAdminAccount SYSTEM_ADMIN_ACCOUNT = new SystemAdminAccount(EMAIL, FORENAME, SURNAME);
    private static final SystemAdminAccount ERRORED_SYSTEM_ADMIN_ACCOUNT = new SystemAdminAccount("abcd", FORENAME,
                                                                                               SURNAME);
    private User expectedUser;
    private PiUser expectedPiUser;

    @BeforeEach
    void setup() {
        expectedUser = new User();
        expectedUser.givenName = FORENAME;
        expectedUser.id = ID;
        expectedUser.displayName = SURNAME;

        expectedPiUser = new PiUser();
        expectedPiUser.setUserId(UUID.randomUUID());
        expectedPiUser.setEmail(EMAIL);
        expectedPiUser.setProvenanceUserId(ID);
        expectedPiUser.setRoles(Roles.SYSTEM_ADMIN);
        expectedPiUser.setUserProvenance(UserProvenances.PI_AAD);

        systemAdminAccountService = new SystemAdminAccountService(validator, azureUserService, userRepository,
                                                                  publicationService, 4,
                                                                  azureAccountService);

    }

    @Test
    void testAddSystemAdminAccount() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        when(azureUserService.createUser(argThat(user -> EMAIL.equals(user.getEmail())), anyBoolean()))
            .thenReturn(expectedUser);
        when(userRepository.save(any())).thenReturn(expectedPiUser);
        when(azureAccountService.retrieveAzureAccount(any()))
            .thenReturn(azUser);
        when(publicationService.sendNotificationEmail(EMAIL, FORENAME, SURNAME)).thenReturn(Boolean.TRUE);
        when(userRepository.findByUserId(any())).thenReturn(Optional.ofNullable(expectedPiUser));
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Set.of());
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser));

        PiUser returnedUser = systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID);

        assertEquals(expectedPiUser, returnedUser, "returned user did not match expected");
    }

    @Test
    void testAddSystemAdminAccountThrowsException() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        when(userRepository.findByUserId(any()))
            .thenReturn(Optional.ofNullable(expectedPiUser));
        when(azureAccountService.retrieveAzureAccount(any()))
            .thenReturn(azUser);
        when(azureUserService.createUser(argThat(user -> EMAIL.equals(user.getEmail())), anyBoolean()))
            .thenThrow(new AzureCustomException("Test error"));

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID));


        assertEquals("Test error",
                     systemAdminAccountException.getErroredSystemAdminAccount().getErrorMessages().get(0),
                     "Error message not as expected");
    }

    @Test
    void testConstraintViolationException() {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        when(userRepository.findByUserId(any()))
            .thenReturn(Optional.ofNullable(expectedPiUser));
        when(azureAccountService.retrieveAzureAccount(any()))
            .thenReturn(azUser);
        when(validator.validate(any())).thenReturn(Set.of(constraintViolation));
        when(constraintViolation.getMessage()).thenReturn("This is a message");
        when(constraintViolation.getPropertyPath()).thenReturn(path);

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(ERRORED_SYSTEM_ADMIN_ACCOUNT, ID));

        assertNotEquals(0, systemAdminAccountException.getErroredSystemAdminAccount().getErrorMessages().size(),
                   "Constraint violation error messages not displayed");
    }

    @Test
    void testAddSystemAdminAccountNotVerified() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);

        expectedPiUser.setRoles(Roles.VERIFIED);
        when(azureUserService.createUser(argThat(user -> EMAIL.equals(user.getEmail())), anyBoolean()))
            .thenReturn(expectedUser);
        when(userRepository.save(any())).thenReturn(expectedPiUser);
        when(publicationService.sendNotificationEmail(EMAIL, FORENAME, SURNAME)).thenReturn(Boolean.FALSE);
        when(userRepository.findByUserId(any())).thenReturn(Optional.ofNullable(expectedPiUser));
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Set.of());
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser));

        PiUser returnedUser = systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID);

        assertEquals(expectedPiUser, returnedUser, "returned user did not match expected");
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
        when(userRepository.findByUserId(any())).thenReturn(Optional.empty());
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Set.of());
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser));

        PiUser returnedUser = systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID);

        assertEquals(expectedPiUser, returnedUser, "returned user did not match expected");
    }

    @Test
    void testUserAlreadyExists() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.PI_AAD))
            .thenReturn(Optional.of(expectedPiUser));
        when(userRepository.findByUserId(any()))
            .thenReturn(Optional.ofNullable(expectedPiUser));
        when(azureAccountService.retrieveAzureAccount(any()))
            .thenReturn(azUser);

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID));

        assertTrue(systemAdminAccountException.getErroredSystemAdminAccount().isDuplicate(), "Duplicate account flag "
            + "not set");
    }

    @Test
    void testAboveMaxAllowsUsers() throws AzureCustomException {
        AzureAccount azUser = new AzureAccount();
        azUser.setDisplayName(FORENAME);
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.PI_AAD))
            .thenReturn(Optional.empty());
        when(userRepository.findByUserId(any()))
            .thenReturn(Optional.ofNullable(expectedPiUser));
        when(azureAccountService.retrieveAzureAccount(any()))
            .thenReturn(azUser);
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser, expectedPiUser,
                                                                                expectedPiUser, expectedPiUser));

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT, ID));

        assertTrue(systemAdminAccountException.getErroredSystemAdminAccount().isAboveMaxSystemAdmin(), "Max system "
            + "admin flag not set");
    }

    @Test
    void testHandleNewSystemAdminAccountAction() {
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser, expectedPiUser));

        ArgumentCaptor<CreateSystemAdminAction> systemAdminAccountArgumentCaptor =
            ArgumentCaptor.forClass(CreateSystemAdminAction.class);

        doNothing().when(publicationService).sendSystemAdminAccountAction(systemAdminAccountArgumentCaptor.capture());

        systemAdminAccountService.handleNewSystemAdminAccountAction(SYSTEM_ADMIN_ACCOUNT, ID, ActionResult.ATTEMPTED,
                                                                    FORENAME);

        CreateSystemAdminAction createSystemAdminAction = systemAdminAccountArgumentCaptor.getValue();

        assertEquals(EMAIL, createSystemAdminAction.getAccountEmail(), "Unknown email retrieved");
        assertEquals(FORENAME, createSystemAdminAction.getRequesterName(), "Unknown requester name retrieved");
        assertEquals(List.of(EMAIL, EMAIL), createSystemAdminAction.getEmailList(), "Unknown email list retrieved");
        assertEquals(ActionResult.ATTEMPTED, createSystemAdminAction.getActionResult(),
                     "Action result not as expected");
    }
}
