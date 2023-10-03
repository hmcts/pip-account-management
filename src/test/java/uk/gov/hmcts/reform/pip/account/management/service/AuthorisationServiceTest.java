package uk.gov.hmcts.reform.pip.account.management.service;

import nl.altindag.log.LogCaptor;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Test
    void testSystemAdminUserCanUpdateAndDeleteSystemAdmin() {
        user.setRoles(Roles.SYSTEM_ADMIN);
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
    void testSystemAdminUserCannotUpdateAndDeleteThirdPartyAccount() {
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_ALL);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCanUpdateAndDeleteSuperAdmin() {
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_CTSC);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

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
    void testSuperAdminUserCanUpdateAndDeleteAdmin() {
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

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
    void testSuperAdminUserCannotUpdateAndDeleteSystemAdmin() {
        user.setRoles(Roles.SYSTEM_ADMIN);
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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testUserCannotUpdateTheirOwnAccount() {
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        PiUser adminUser = new PiUser();
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

            softly.assertThat(logCaptor.getErrorLogs().get(0))
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
}
