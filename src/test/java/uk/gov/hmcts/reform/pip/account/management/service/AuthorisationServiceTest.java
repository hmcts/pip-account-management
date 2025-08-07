package uk.gov.hmcts.reform.pip.account.management.service;

import nl.altindag.log.LogCaptor;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorisationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ANOTHER_USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID2 = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID3 = UUID.randomUUID();

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

    private static final String CAN_DELETE_SUBSCRIPTION_MESSAGE = "User should be able to delete subscription";
    private static final String CANNOT_DELETE_SUBSCRIPTION_MESSAGE = "User should not be able to delete subscription";
    private static final String ERROR_LOG = "User %s is forbidden to remove subscription with ID %s belongs to "
        + "another user %s";

    private static final String LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String LOG_MATCHED_MESSAGE = "Error log message does not match";
    private static final String EXCEPTION_MATCHED_MESSAGE = "Exception message does not match";

    private static final String UNAUTHORIZED_MESSAGE = "User should not be able to perform action when unauthorised";

    private static PiUser user = new PiUser();
    private static PiUser adminUser = new PiUser();

    private static Subscription subscription = new Subscription();
    private static Subscription subscription2 = new Subscription();
    private static Subscription subscription3 = new Subscription();

    private static final String ADMIN_ROLE = "APPROLE_api.request.admin";
    private static final String UNKNOWN_ROLE = "APPROLE_api.request.unknown";
    private static final String TEST_USER_ID = "123";

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuthorisationService authorisationService;

    @BeforeEach
    void beforeEachSetup() {
        SecurityContextHolder.setContext(securityContext);
    }

    @BeforeAll
    static void setup() {
        user.setUserId(USER_ID);
        adminUser.setUserId(ADMIN_USER_ID);

        subscription.setId(SUBSCRIPTION_ID);
        subscription2.setId(SUBSCRIPTION_ID2);
        subscription3.setId(SUBSCRIPTION_ID3);
    }

    private void setupWithAuth() {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(ADMIN_ROLE)
        );

        Authentication auth = new TestingAuthenticationToken(TEST_USER_ID, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
    }

    private void setupWithoutAuth() {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(UNKNOWN_ROLE)
        );

        Authentication auth = new TestingAuthenticationToken(TEST_USER_ID, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testSystemAdminCanCreateAnyRole(Roles role) {
        setupWithAuth();
        user.setRoles(role);
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

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testSystemAdminCannotCreateAnyRoleWhenUnauthorized(Roles role) {
        setupWithoutAuth();
        user.setRoles(role);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        lenient().when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        assertThat(authorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
            .as(UNAUTHORIZED_MESSAGE)
            .isFalse();
    }

    @Test
    void testSuperAdminCannotCreateThirdPartyRole() {
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
    void testSystemAdminUserCanDeleteSystemAdmin() {
        setupWithAuth();
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
    void testSystemAdminUserCannotUpdateAndDeleteSystemAdminWhenUnauthorized() {
        setupWithoutAuth();
        user.setRoles(Roles.SYSTEM_ADMIN);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.SYSTEM_ADMIN);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
            .as(UNAUTHORIZED_MESSAGE)
            .isFalse();

        softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(UNAUTHORIZED_MESSAGE)
            .isFalse();

        softly.assertAll();
    }

    @Test
    void testSystemAdminUserCanUpdateAndDeleteSuperAdmin() {
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
    void testSuperAdminUserCannotUpdateAndDeleteSuperAdmin() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_CTSC);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
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
    void testSuperAdminUserCannotUpdateAndDeleteAdmin() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
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
    void testSuperAdminUserCannotUpdateAndDeleteSystemAdmin() {
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
        setupWithAuth();
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
    void testSsoUserCanBeUpdatedAndDeleted() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.SSO);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(authorisationService.userCanDeleteAccount(USER_ID, null))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isTrue();

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
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_SUPER_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);

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
        setupWithAuth();
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(EXCEPTION_MATCHED_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User with supplied user id: %s could not be found", USER_ID));

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testExceptionThrowIfAdminUserNotFound() {
        setupWithAuth();
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(EXCEPTION_MATCHED_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User with supplied user id: %s could not be found", ADMIN_USER_ID));
    }

    @Test
    void testUserCanCreateSystemAdmin() {
        setupWithAuth();
        UUID userId = UUID.randomUUID();
        PiUser user = new PiUser();
        user.setRoles(Roles.SYSTEM_ADMIN);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        assertThat(authorisationService.userCanCreateSystemAdmin(userId)).isTrue();
    }

    @Test
    void testUserCannotCreateSystemAdminWhenUnauthorized() {
        setupWithoutAuth();
        UUID userId = UUID.randomUUID();
        assertThat(authorisationService.userCanCreateSystemAdmin(userId)).isFalse();
    }

    @Test
    void testUserCannotCreateSystemAdminIfAccountNotFound() {
        setupWithAuth();
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanCreateSystemAdmin(userId)).isFalse();

            assertThat(logCaptor.getErrorLogs().get(0)).contains(
                String.format("User with ID %s is forbidden to create a B2C system admin", userId));
        }
    }

    @Test
    void testUserCannotCreateSystemAdminIfUserIsNotSystemAdmin() {
        setupWithAuth();
        UUID userId = UUID.randomUUID();
        PiUser user = new PiUser();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanCreateSystemAdmin(userId)).isFalse();

            assertThat(logCaptor.getErrorLogs().get(0)).contains(
                String.format("User with ID %s is forbidden to create a B2C system admin", userId));
        }
    }

    @Test
    void testSystemAdminUserCanDeleteSubscription() {
        setupWithAuth();
        user.setRoles(Roles.SYSTEM_ADMIN);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            verifyNoInteractions(subscriptionRepository);
            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testSystemAdminUserCannotDeleteSubscriptionWhenUnauthorized() {
        setupWithoutAuth();
        user.setRoles(Roles.SYSTEM_ADMIN);

        assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
            .as(UNAUTHORIZED_MESSAGE)
            .isFalse();

        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void testAdminUserCannotDeleteSubscriptionIfUserMismatched() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(ANOTHER_USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, USER_ID, SUBSCRIPTION_ID, ANOTHER_USER_ID));
        }
    }

    @Test
    void testVerifiedUserCannotDeleteSubscriptionIfUserMismatched() {
        setupWithAuth();
        user.setRoles(Roles.VERIFIED);
        subscription.setUserId(ANOTHER_USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, USER_ID, SUBSCRIPTION_ID, ANOTHER_USER_ID));
        }
    }

    @Test
    void testUserCanDeleteSubscriptionIfUserMatchedInSingleSubscription() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testUserCanDeleteSubscriptionIfUserMatchedInAllSubscriptions() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(USER_ID);
        subscription2.setUserId(USER_ID);
        subscription3.setUserId(USER_ID);

        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID2)).thenReturn(Optional.of(subscription2));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID3)).thenReturn(Optional.of(subscription3));

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID, SUBSCRIPTION_ID2,
                                                                       SUBSCRIPTION_ID3))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testUserCannotDeleteSubscriptionIfUserMisMatchedInSomeOfTheSubscriptions() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        subscription.setUserId(USER_ID);
        subscription2.setUserId(ANOTHER_USER_ID);
        subscription3.setUserId(USER_ID);

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            when(accountService.getUserById(USER_ID)).thenReturn(user);
            when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
            when(subscriptionRepository.findById(SUBSCRIPTION_ID2)).thenReturn(Optional.of(subscription2));

            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID, SUBSCRIPTION_ID2,
                                                                       SUBSCRIPTION_ID3
            ))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ERROR_LOG, USER_ID, SUBSCRIPTION_ID2, ANOTHER_USER_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID3);
        }
    }

    @Test
    void testUserCanDeleteSubscriptionReturnsTrueIfSubscriptionNotFound() {
        setupWithAuth();
        user.setRoles(Roles.INTERNAL_ADMIN_LOCAL);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.empty());

        try (LogCaptor logCaptor = LogCaptor.forClass(AuthorisationService.class)) {
            assertThat(authorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }
}
