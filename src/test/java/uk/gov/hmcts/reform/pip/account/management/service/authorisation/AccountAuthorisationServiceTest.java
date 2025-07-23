package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import nl.altindag.log.LogCaptor;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.GENERAL_THIRD_PARTY;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_LOCAL;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_SUPER_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_SUPER_ADMIN_LOCAL;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.Roles.VERIFIED;
import static uk.gov.hmcts.reform.pip.model.account.Roles.VERIFIED_THIRD_PARTY_ALL;
import static uk.gov.hmcts.reform.pip.model.account.Roles.VERIFIED_THIRD_PARTY_PRESS;

@SuppressWarnings("PMD.UnitTestAssertionsShouldIncludeMessage")
@ExtendWith(MockitoExtension.class)
class AccountAuthorisationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String INTERNAL_ADMIN_CTSC_ROLE = "INTERNAL_ADMIN_CTSC";

    private static final String UNAUTHORIZED_MESSAGE = "User should not be able to perform action when unauthorised";
    private static final String DELETE_ERROR_LOG = "User with ID %s is not authorised to delete this account";
    private static final String UPDATE_ERROR_LOG = "User with ID %s is forbidden to update user with ID %s";
    private static final String UPDATE_OWN_ACCOUNT_ERROR_LOG =
        "User with ID %s is forbidden to update their own account";
    private static final String CAN_DELETE_ACCOUNT_MESSAGE = "User should be able to delete account";
    private static final String CANNOT_DELETE_ACCOUNT_MESSAGE = "User should not be able to delete account";
    private static final String CAN_UPDATE_ACCOUNT_MESSAGE = "User should be able to update account";
    private static final String CANNOT_UPDATE_ACCOUNT_MESSAGE = "User should not be able to update account";
    private static final String CANNOT_CREATE_ACCOUNT_MESSAGE = "User should not be able to create account";
    private static final String LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String LOG_MATCHED_MESSAGE = "Error log message does not match";
    private static final String EXCEPTION_MATCHED_MESSAGE = "Exception message does not match";

    private static PiUser user = new PiUser();
    private static PiUser adminUser = new PiUser();

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private AuthorisationCommonService authorisationCommonService;

    @InjectMocks
    private AccountAuthorisationService accountAuthorisationService;

    @BeforeEach
    void beforeEachSetup() {
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);
    }

    @BeforeAll
    static void setup() {
        user.setUserId(USER_ID);
        adminUser.setUserId(ADMIN_USER_ID);
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testSystemAdminUserCanDeleteAccount(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        assertTrue(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID));
    }

    @Test
    void testSystemAdminUserCanNotDeleteTheirOwnAccount() {
        adminUser.setRoles(SYSTEM_ADMIN);

        assertFalse(accountAuthorisationService.userCanDeleteAccount(ADMIN_USER_ID, ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { SYSTEM_ADMIN_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotDeleteAccountWhenNotSystemAdmin(Roles role) {
        adminUser.setRoles(role);

        assertFalse(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID));
    }

    @Test
    void testSystemAdminUserCanNotDeleteAccountWhenNotLoggedIn() {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(VERIFIED);

        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertFalse(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID));
    }

    @Test
    void testSystemAdminUserCanViewAccounts() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        assertTrue(accountAuthorisationService.userCanViewAccounts(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { SYSTEM_ADMIN_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotViewAccountsWhenNotSystemAdmin(Roles role) {
        user.setRoles(role);

        assertFalse(accountAuthorisationService.userCanViewAccounts(USER_ID));
    }

    @Test
    void testSystemAdminUserCanNotViewAccountsWhenNotLoggedIn() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertFalse(accountAuthorisationService.userCanViewAccounts(ADMIN_USER_ID));
    }

    @Test
    void testSystemAdminUserCanBulkCreateMediaAccounts() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        assertTrue(accountAuthorisationService.userCanBulkCreateMediaAccounts(ADMIN_USER_ID));
    }

    @Test
    void testSystemAdminUserCanNotBulkCreateMediaAccountsWhenNotLoggedIn() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertFalse(accountAuthorisationService.userCanBulkCreateMediaAccounts(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { SYSTEM_ADMIN_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotBulkCreateMediaAccountsWhenNotSystemAdmin(Roles role) {
        user.setRoles(role);

        assertFalse(accountAuthorisationService.userCanBulkCreateMediaAccounts(USER_ID));
    }

    @Test
    void testAdminCtscUserCanCreateAzureAccount() {
        adminUser.setRoles(INTERNAL_ADMIN_CTSC);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        assertTrue(accountAuthorisationService.userCanCreateAzureAccount(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { INTERNAL_ADMIN_CTSC_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotCreateAzureAccountWhenNotAdminCtsc(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        assertFalse(accountAuthorisationService.userCanCreateAzureAccount(USER_ID));
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testUserCanNotCreateAzureAccountWhenNotLoggedIn(Roles role) {
        user.setRoles(role);

        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertFalse(accountAuthorisationService.userCanCreateAzureAccount(USER_ID));
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testSystemAdminCanCreateThirdPartyUsers(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);

        lenient().when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.ofNullable(adminUser));
        lenient().when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.ofNullable(user));

        assertTrue(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { SYSTEM_ADMIN_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotCreateThirdPartyUserWhenNotSystemAdmin(Roles role) {
        adminUser.setRoles(role);
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        user.setRoles(GENERAL_THIRD_PARTY);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testUserCanNotCreateThirdPartyUserAccountWhenNotLoggedIn(Roles role) {
        adminUser.setRoles(role);
        user.setRoles(GENERAL_THIRD_PARTY);

        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertFalse(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)));
    }

    @Test
    void testAdminCtscCanCreatePiAadVerifiedUser() {
        adminUser.setRoles(INTERNAL_ADMIN_CTSC);
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        user.setRoles(VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);

        assertTrue(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { INTERNAL_ADMIN_CTSC_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUserCanNotCreatePiAadVerifiedUserWhenNotAdminCtsc(Roles role) {
        adminUser.setRoles(role);
        adminUser.setUserId(ADMIN_USER_ID);
        user.setRoles(VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);

        lenient().when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        assertFalse(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)));
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testUserCanNotCreatePiAadVerifiedUserAccountWhenNotLoggedIn(Roles role) {
        adminUser.setRoles(role);
        user.setRoles(VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertFalse(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)));
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testSystemAdminCannotCreateAnyRoleWhenUnauthorized(Roles role) {
        user.setRoles(role);
        adminUser.setRoles(SYSTEM_ADMIN);

        lenient().when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        assertThat(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
            .as(UNAUTHORIZED_MESSAGE)
            .isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { "VERIFIED" }, mode = EnumSource.Mode.EXCLUDE)
    void testSystemAdminCanCreateAnyRoleExceptPiAadVerifiedUser(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);

        lenient().when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);

        assertTrue(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)));
    }

    @Test
    void testThirdPartyUserCannotCreateThirdPartyRole() {
        user.setRoles(VERIFIED_THIRD_PARTY_PRESS);
        adminUser.setRoles(VERIFIED_THIRD_PARTY_PRESS);

        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanCreateAccount(ADMIN_USER_ID, List.of(user)))
                .as(CANNOT_CREATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateSystemAdmin() {
        user.setRoles(SYSTEM_ADMIN);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCannotUpdateSystemAdminWhenUnauthorized() {
        user.setRoles(SYSTEM_ADMIN);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(SYSTEM_ADMIN);

        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(false);

        SoftAssertions softly = new SoftAssertions();

        softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(UNAUTHORIZED_MESSAGE)
            .isFalse();

        softly.assertAll();
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testSystemAdminUserCanUpdateAnyRoles(Roles role) {
        user.setRoles(role);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateAdmin() {
        user.setRoles(INTERNAL_ADMIN_CTSC);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateVerifiedAccount() {
        user.setRoles(VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_UPDATE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();

            softly.assertAll();
        }
    }

    @Test
    void testSystemAdminUserCanUpdateThirdPartyAccount() {
        user.setRoles(VERIFIED_THIRD_PARTY_ALL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
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
        user.setRoles(INTERNAL_SUPER_ADMIN_CTSC);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
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
        user.setRoles(INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
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
        user.setRoles(SYSTEM_ADMIN);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCannotUpdateAndDeleteVerifiedAccount() {
        user.setRoles(VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testSuperAdminUserCannotUpdateAndDeleteThirdPartyAccount() {
        user.setRoles(GENERAL_THIRD_PARTY);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(INTERNAL_SUPER_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testAdminUserCannotUpdateAndDeleteAccount() {
        user.setRoles(INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(INTERNAL_ADMIN_LOCAL);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testVerifiedUserCannotUpdateAndDeleteAccount() {
        user.setRoles(VERIFIED);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(VERIFIED);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testThirdPartyUserCannotUpdateAndDeleteAccount() {
        user.setRoles(INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);
        adminUser.setRoles(GENERAL_THIRD_PARTY);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.of(adminUser));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_DELETE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_UPDATE_ACCOUNT_MESSAGE)
                .isFalse();

            softly.assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            softly.assertThat(logCaptor.getErrorLogs().get(0))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID));

            softly.assertThat(logCaptor.getErrorLogs().get(1))
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, ADMIN_USER_ID, USER_ID));

            softly.assertAll();
        }
    }

    @Test
    void testSsoUserCanBeUpdatedAndDeleted() {
        user.setRoles(INTERNAL_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.SSO);
        adminUser.setRoles(SYSTEM_ADMIN);

        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanDeleteAccount(USER_ID, ADMIN_USER_ID))
                .as(CAN_DELETE_ACCOUNT_MESSAGE)
                .isTrue();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, null))
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
        user.setRoles(INTERNAL_SUPER_ADMIN_LOCAL);
        user.setUserProvenance(UserProvenances.PI_AAD);

        PiUser adminUser = new PiUser();
        adminUser.setUserId(USER_ID);
        adminUser.setRoles(INTERNAL_SUPER_ADMIN_LOCAL);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            SoftAssertions softly = new SoftAssertions();

            softly.assertThat(accountAuthorisationService.userCanUpdateAccount(USER_ID, USER_ID))
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

        assertThatThrownBy(() -> accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(EXCEPTION_MATCHED_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User with supplied user id: %s could not be found", USER_ID));

        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void testExceptionThrowIfAdminUserNotFound() {
        when(userRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByUserId(ADMIN_USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountAuthorisationService.userCanUpdateAccount(USER_ID, ADMIN_USER_ID))
            .as(EXCEPTION_MATCHED_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User with supplied user id: %s could not be found", ADMIN_USER_ID));
    }

    @Test
    void testUserCanCreateSystemAdmin() {
        UUID userId = UUID.randomUUID();
        PiUser user = new PiUser();
        user.setRoles(SYSTEM_ADMIN);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));

        assertThat(accountAuthorisationService.userCanCreateSystemAdmin(userId)).isTrue();
    }

    @Test
    void testUserCannotCreateSystemAdminWhenUnauthorized() {
        UUID userId = UUID.randomUUID();
        assertThat(accountAuthorisationService.userCanCreateSystemAdmin(userId)).isFalse();
    }

    @Test
    void testUserCannotCreateSystemAdminIfAccountNotFound() {
        UUID userId = UUID.randomUUID();

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            assertThat(accountAuthorisationService.userCanCreateSystemAdmin(userId)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is forbidden to create a B2C system admin", userId));
        }
    }

    @Test
    void testUserCannotCreateSystemAdminIfUserIsNotSystemAdmin() {
        UUID userId = UUID.randomUUID();
        PiUser user = new PiUser();
        user.setRoles(INTERNAL_ADMIN_LOCAL);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(user));
        when(authorisationCommonService.hasOAuthAdminRole()).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountAuthorisationService.class)) {
            assertThat(accountAuthorisationService.userCanCreateSystemAdmin(userId)).isFalse();

            assertThat(logCaptor.getErrorLogs().getFirst()).contains(
                String.format("User with ID %s is forbidden to create a B2C system admin", userId));
        }
    }
}
