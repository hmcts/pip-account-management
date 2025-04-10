package uk.gov.hmcts.reform.pip.account.management.service;

import nl.altindag.log.LogCaptor;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"PMD.TooManyMethods", "PMD.JUnitTestsShouldIncludeAssert"})
class AuthorisationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();

    private static final String DELETE_ERROR_LOG = "User with ID %s is forbidden to remove user with ID %s";
    private static final String UPDATE_ERROR_LOG = "User with ID %s is forbidden to update user with ID %s";
    private static final String UPDATE_OWN_ACCOUNT_ERROR_LOG =
        "User with ID %s is forbidden to update their own account";

    private static final String CAN_DELETE_ACCOUNT_MESSAGE = "User should be able to delete account";
    private static final String CANNOT_DELETE_ACCOUNT_MESSAGE = "User should not be able to delete account";

    private static final String CAN_UPDATE_ACCOUNT_MESSAGE = "User should be able to update account";
    private static final String CANNOT_UPDATE_ACCOUNT_MESSAGE = "User should not be able to update account";

    private static final String CAN_CREATE_ACCOUNT_MESSAGE = "User should be able to create account";
    private static final String CANNOT_CREATE_ACCOUNT_MESSAGE = "User should not be able to create account";

    private static final String LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String LOG_MATCHED_MESSAGE = "Error log message does not match";
    private static final String EXCEPTION_MATCHED_MESSAGE = "Exception message does not match";

    private static PiUser user = new PiUser();
    private static PiUser adminUser = new PiUser();

    @Mock
    UserRepository userRepository;

    @InjectMocks
    AuthorisationService authorisationService;

    @BeforeAll
    static void setup() {
        user.setUserId(USER_ID);
        adminUser.setUserId(ADMIN_USER_ID);
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { "VERIFIED" }, mode = EnumSource.Mode.EXCLUDE)
    void testSystemAdminCanCreateAnyRole(Roles role) {
        user.setRoles(role);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        lenient().when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CAN_CREATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_MATCHED_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminCannotCreateThirdPartyRole() {
        user.setRoles(Roles.GENERAL_THIRD_PARTY);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_CTSC);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testAdminCannotCreateThirdPartyRole() {
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_ALL);
        adminUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testVerifiedUserCannotCreateThirdPartyRole() {
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_CFT);
        adminUser.setRoles(Roles.VERIFIED);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testThirdPartyUserCannotCreateThirdPartyRole() {
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_PRESS);
        adminUser.setRoles(Roles.VERIFIED_THIRD_PARTY_PRESS);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateAndDeleteSystemAdmin() {
        user.setRoles(Roles.SYSTEM_ADMIN);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateAndDeleteSuperAdmin() {
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateAndDeleteAdmin() {
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateAndDeleteVerifiedAccount() {
        user.setRoles(Roles.VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateAndDeleteThirdPartyAccount() {
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_ALL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCanUpdateSuperAdmin() {
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_CTSC);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCanUpdateAdmin() {
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCannotUpdateAndDeleteSystemAdmin() {
        user.setRoles(Roles.SYSTEM_ADMIN);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCannotUpdateAndDeleteVerifiedAccount() {
        user.setRoles(Roles.VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCannotUpdateAndDeleteThirdPartyAccount() {
        user.setRoles(Roles.GENERAL_THIRD_PARTY);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testAdminUserCannotUpdateAndDeleteAccount() {
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testVerifiedUserCannotUpdateAndDeleteAccount() {
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.VERIFIED);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testThirdPartyUserCannotUpdateAndDeleteAccount() {
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.GENERAL_THIRD_PARTY);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testSsoUserCanBeUpdated() {
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.SSO);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, null))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testUserCannotUpdateTheirOwnAccount() {
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);

        adminUser.setUserId(USER_ID);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_OWN_ACCOUNT_ERROR_LOG, USER_ID));

            softly.assertAll();

            verifyNoInteractions(userRepository);
        }
    }

    @Test
    void testExceptionThrowIfUserNotFound() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(EXCEPTION_MATCHED_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User with supplied user id: %s could not be found", USER_ID));

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testExceptionThrowIfAdminUserNotFound() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(EXCEPTION_MATCHED_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User with supplied user id: %s could not be found", ADMIN_USER_ID));
    }

    @Test
    void testUserCanCreateSystemAdmin() {
        user.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        assertThat(authorisationService.userCanCreateSystemAdmin(USER_ID)).isTrue();
    }

    @Test
    void testUserCannotCreateSystemAdminIfAccountNotFound() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanCreateSystemAdmin(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is forbidden to create a B2C system admin", USER_ID));
        }
    }

    @Test
    void testUserCannotCreateSystemAdminIfUserIsNotSystemAdmin() {
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanCreateSystemAdmin(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is forbidden to create a B2C system admin", USER_ID));
        }
    }

    @Test
    void testUserIsSystemAdminShouldReturnTrue() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        assertThat(authorisationService.userIsSystemAdmin(USER_ID)).isTrue();
    }

    @Test
    void testUserIsSystemAdminShouldReturnFalse() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.VERIFIED);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        assertThat(authorisationService.userIsSystemAdmin(USER_ID)).isFalse();
    }

    @Test
    void testUserIsCtscAdminShouldReturnTrue() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        assertThat(authorisationService.userIsAdminCTSC(USER_ID)).isTrue();
    }

    @Test
    void testUserIsCtscAdminShouldReturnFalse() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        assertThat(authorisationService.userIsAdminCTSC(USER_ID)).isFalse();
    }

    @Test
    void testUserForbiddenToUpdateMediaApplications() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanUpdateMediaApplications(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is not authorised to update media applications", USER_ID));
        }
    }

    @Test
    void testUserForbiddenToViewMediaApplications() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanViewMediaApplications(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is not authorised to view media applications", USER_ID));
        }
    }

    @Test
    void testUserForbiddenToCreateAzureAccounts() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanCreateAzureAccount(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is not authorised to create accounts", USER_ID));
        }
    }

    @Test
    void testUserForbiddenToBulkCreateMediaAccounts() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanBulkCreateMediaAccounts(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is not authorised to create media accounts", USER_ID));
        }
    }

    @Test
    void testUserForbiddenToViewAccountDetails() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanViewAccountDetails(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is not authorised to view account details", USER_ID));
        }
    }

    @Test
    void testUserForbiddenToViewAuditLogs() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanRequestAuditLogs(USER_ID)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is not authorised to view audit logs", USER_ID));
        }
    }

    @Test
    void testSystemAdminUserCannotDeleteTheirOwnAccount() {
        user.setUserId(USER_ID);
        user.setRoles(Roles.SYSTEM_ADMIN);
        user.setUserProvenance(UserProvenances.PI_AAD);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(user.getUserId(), user.getUserId()))
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testCtscAdminCanCreatePiAadVerifiedRole() {
        user.setRoles(Roles.VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CAN_CREATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_MATCHED_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminCannotCreatePiAadVerifiedRole() {
        user.setRoles(Roles.VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminCannotCreatePiAadVerifiedRole() {
        user.setRoles(Roles.VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testVerifiedUserCannotCreatePiAadVerifiedRole() {
        user.setRoles(Roles.VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.VERIFIED);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testThirdPartyUserCannotCreatePiAadVerifiedRole() {
        user.setRoles(Roles.VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.VERIFIED_THIRD_PARTY_PRESS);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

}
