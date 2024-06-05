package uk.gov.hmcts.reform.pip.account.management.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SystemAdminAccountException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemAdminAccountServiceTest {
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
    private PiUser expectedPiUser;

    @BeforeEach
    void setup() {
        expectedPiUser = new PiUser();
        expectedPiUser.setUserId(UUID.randomUUID());
        expectedPiUser.setEmail(EMAIL);
        expectedPiUser.setProvenanceUserId(ID);
        expectedPiUser.setRoles(Roles.SYSTEM_ADMIN);
        expectedPiUser.setUserProvenance(UserProvenances.SSO);

        systemAdminAccountService = new SystemAdminAccountService(validator, userRepository, 4);

    }

    @Test
    void testAddSystemAdminAccountSuccess() {
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Collections.emptySet());
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.SSO))
            .thenReturn(Optional.empty());
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser));
        when(userRepository.save(any())).thenReturn(expectedPiUser);

        PiUser returnedUser = systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT);

        assertEquals(expectedPiUser, returnedUser, "returned user did not match expected");
    }

    @Test
    void testAddSystemAdminAccountConstraintViolation() {
        when(validator.validate(any())).thenReturn(Set.of(constraintViolation));
        when(constraintViolation.getMessage()).thenReturn("This is a message");
        when(constraintViolation.getPropertyPath()).thenReturn(path);

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(ERRORED_SYSTEM_ADMIN_ACCOUNT));

        assertNotEquals(0, systemAdminAccountException.getErroredSystemAdminAccount().getErrorMessages().size(),
                        "Constraint violation error messages not displayed");

        verify(userRepository, never()).save(any());
    }

    @Test
    void testAddSystemAdminAccountUserAlreadyExists() {
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Collections.emptySet());
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.SSO))
            .thenReturn(Optional.of(expectedPiUser));

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT));

        assertTrue(systemAdminAccountException.getErroredSystemAdminAccount().isDuplicate(),
                   "Duplicate account flag not set");

        verify(userRepository, never()).save(any());
    }

    @Test
    void testAddSystemAdminAccountAboveMaxAllowsUsers() {
        when(validator.validate(SYSTEM_ADMIN_ACCOUNT)).thenReturn(Collections.emptySet());
        when(userRepository.findByEmailAndUserProvenance(EMAIL, UserProvenances.SSO))
            .thenReturn(Optional.empty());
        when(userRepository.findByRoles(Roles.SYSTEM_ADMIN)).thenReturn(List.of(expectedPiUser, expectedPiUser,
                                                                                expectedPiUser, expectedPiUser));

        SystemAdminAccountException systemAdminAccountException =
            assertThrows(SystemAdminAccountException.class, () ->
                systemAdminAccountService.addSystemAdminAccount(SYSTEM_ADMIN_ACCOUNT));

        assertTrue(systemAdminAccountException.getErroredSystemAdminAccount().isAboveMaxSystemAdmin(),
                   "Max system admin flag not set");

        verify(userRepository, never()).save(any());
    }
}
