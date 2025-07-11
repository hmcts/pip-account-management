package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;

@SuppressWarnings("PMD.AvoidDuplicateLiterals")
@ExtendWith(MockitoExtension.class)
class SubscriptionAuthorisationServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ANOTHER_USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID2 = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID3 = UUID.randomUUID();
    private static final String SYSTEM_ADMIN_ROLE = "SYSTEM_ADMIN";
    private static final String INTERNAL_SUPER_ADMIN_CTSC_ROLE = "INTERNAL_SUPER_ADMIN_CTSC";
    private static final String INTERNAL_SUPER_ADMIN_LOCAL_ROLE = "INTERNAL_SUPER_ADMIN_LOCAL";
    private static final String INTERNAL_ADMIN_CTSC_ROLE = "INTERNAL_ADMIN_CTSC";
    private static final String INTERNAL_ADMIN_LOCAL_ROLE = "INTERNAL_ADMIN_LOCAL";

    private static final String CAN_VIEW_SUBSCRIPTION_MESSAGE = "User should be able to view subscription";
    private static final String CANNOT_VIEW_SUBSCRIPTION_MESSAGE = "User should not be able to view subscription";
    private static final String CAN_ADD_SUBSCRIPTION_MESSAGE = "User should be able to add subscription";
    private static final String CANNOT_ADD_SUBSCRIPTION_MESSAGE = "User should not be able to add subscription";
    private static final String CAN_DELETE_SUBSCRIPTION_MESSAGE = "User should be able to delete subscription";
    private static final String CANNOT_DELETE_SUBSCRIPTION_MESSAGE = "User should not be able to delete subscription";
    private static final String CAN_UPDATE_SUBSCRIPTION_MESSAGE = "User should be able to update subscription";
    private static final String CANNOT_UPDATE_SUBSCRIPTION_MESSAGE = "User should not be able to update subscription";
    private static final String CAN_GET_CHANNELS_MESSAGE = "User should be able to view subscription channels";
    private static final String CANNOT_GET_CHANNELS_MESSAGE = "User should be able to view subscription channels";
    private static final String MISMATCH_ERROR_LOG = "User %s is forbidden to alter subscription with ID %s belongs to "
        + "another user %s";
    private static final String DELETE_ERROR_LOG = "User with ID %s is not authorised to remove these subscriptions";
    private static final String UPDATE_ERROR_LOG = "User with ID %s is not authorised to update subscriptions for user "
        + "with ID %s";
    private static final String ADD_ERROR_LOG = "User with ID %s is not authorised to add subscription with ID %s";
    private static final String VIEW_ERROR_LOG = "User with ID %s is not authorised to view these subscriptions";
    private static final String CHANNEL_ERROR_LOG = "User with ID %s is not authorised to retrieve these channels";
    private static final String UNAUTHORIZED_MESSAGE = "User should not be able to perform action when unauthorised";
    private static final String LOG_EMPTY_MESSAGE = "Error log should be empty";
    private static final String LOG_NOT_EMPTY_MESSAGE = "Error log should not be empty";
    private static final String LOG_MATCHED_MESSAGE = "Error log message does not match";

    private static PiUser user = new PiUser();
    private static PiUser adminUser = new PiUser();

    private static Subscription subscription = new Subscription();
    private static Subscription subscription2 = new Subscription();
    private static Subscription subscription3 = new Subscription();

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private AuthorisationCommonService authorisationCommonService;

    @InjectMocks
    private SubscriptionAuthorisationService subscriptionAuthorisationService;

    @BeforeAll
    static void setup() {
        user.setUserId(USER_ID);
        adminUser.setUserId(ADMIN_USER_ID);

        subscription.setId(SUBSCRIPTION_ID);
        subscription2.setId(SUBSCRIPTION_ID2);
        subscription3.setId(SUBSCRIPTION_ID3);
    }

    @ParameterizedTest
    @EnumSource(Roles.class)
    void testSystemAdminUserCanViewSubscriptions(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanViewSubscriptions(ADMIN_USER_ID, USER_ID))
                .as(CAN_VIEW_SUBSCRIPTION_MESSAGE)
                .isTrue();

            verifyNoInteractions(subscriptionRepository);
            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @Test
    void testSystemAdminUserCanNotViewSubscriptionsWhenNotLoggedIn() {
        adminUser.setRoles(SYSTEM_ADMIN);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanViewSubscriptions(ADMIN_USER_ID, USER_ID))
                .as(CANNOT_VIEW_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(VIEW_ERROR_LOG, ADMIN_USER_ID));
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanViewTheirOwnSubscriptions(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanViewSubscriptions(USER_ID, USER_ID))
                .as(CAN_VIEW_SUBSCRIPTION_MESSAGE)
                .isTrue();

            verifyNoInteractions(subscriptionRepository);
            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanNotViewSubscriptionsIfUserMismatched(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanViewSubscriptions(USER_ID, ADMIN_USER_ID))
                .as(CANNOT_VIEW_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(VIEW_ERROR_LOG, USER_ID));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = { SYSTEM_ADMIN_ROLE }, mode = EnumSource.Mode.EXCLUDE)
    void testUnauthorisedUserCanNotViewSubscriptions(Roles role) {
        adminUser.setRoles(role);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanViewSubscriptions(ADMIN_USER_ID, USER_ID))
                .as(CANNOT_VIEW_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(VIEW_ERROR_LOG, ADMIN_USER_ID));
        }
    }

    @Test
    void testSystemAdminUserCanDeleteSubscription() {
        adminUser.setRoles(SYSTEM_ADMIN);

        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteSubscriptions(ADMIN_USER_ID, SUBSCRIPTION_ID))
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
        user.setRoles(SYSTEM_ADMIN);

        assertThat(subscriptionAuthorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
            .as(UNAUTHORIZED_MESSAGE)
            .isFalse();

        verifyNoInteractions(subscriptionRepository);
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanDeleteTheirOwnSubscriptions(Roles role) {
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCannotDeleteSubscriptionIfUserMismatched(Roles role) {
        user.setRoles(role);
        subscription.setUserId(ANOTHER_USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(MISMATCH_ERROR_LOG, USER_ID, SUBSCRIPTION_ID, ANOTHER_USER_ID));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        INTERNAL_SUPER_ADMIN_CTSC_ROLE,
        INTERNAL_SUPER_ADMIN_LOCAL_ROLE,
        INTERNAL_ADMIN_CTSC_ROLE,
        INTERNAL_ADMIN_LOCAL_ROLE,
    }, mode = EnumSource.Mode.INCLUDE)
    void testUnauthorisedUserCannotDeleteSubscription(Roles role) {
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, USER_ID));
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanAddSubscriptionToTheirAccount(Roles role) {
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanAddSubscriptions(USER_ID, subscription))
                .as(CAN_ADD_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanNotAddSubscriptionToOtherAccounts(Roles role) {
        user.setRoles(role);
        subscription.setId(SUBSCRIPTION_ID);
        subscription.setUserId(ANOTHER_USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanAddSubscriptions(USER_ID, subscription))
                .as(CANNOT_ADD_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(MISMATCH_ERROR_LOG, USER_ID, SUBSCRIPTION_ID, ANOTHER_USER_ID));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        INTERNAL_SUPER_ADMIN_CTSC_ROLE,
        INTERNAL_SUPER_ADMIN_LOCAL_ROLE,
        INTERNAL_ADMIN_CTSC_ROLE,
        INTERNAL_ADMIN_LOCAL_ROLE,
        SYSTEM_ADMIN_ROLE
    }, mode = EnumSource.Mode.INCLUDE)
    void testNonVerifiedUserCanNotAddSubscriptionToOwnAccount(Roles role) {
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanAddSubscriptions(USER_ID, subscription))
                .as(CANNOT_ADD_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ADD_ERROR_LOG, USER_ID, SUBSCRIPTION_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID);
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testSystemAdminUserCanAddSubscriptionToThirdPartyAccount(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanAddSubscriptions(ADMIN_USER_ID, subscription))
                .as(CAN_ADD_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testSystemAdminUserCanNotAddSubscriptionToThirdPartyAccountWhenNotLoggedIn(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanAddSubscriptions(ADMIN_USER_ID, subscription))
                .as(CANNOT_ADD_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ADD_ERROR_LOG, ADMIN_USER_ID, SUBSCRIPTION_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID);
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        "VERIFIED",
        INTERNAL_SUPER_ADMIN_CTSC_ROLE,
        INTERNAL_SUPER_ADMIN_LOCAL_ROLE,
        INTERNAL_ADMIN_CTSC_ROLE,
        INTERNAL_ADMIN_LOCAL_ROLE,
        "SYSTEM_ADMIN"
    }, mode = EnumSource.Mode.INCLUDE)
    void testSystemAdminUserCanNotAddSubscriptionToNonThirdPartyAccounts(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanAddSubscriptions(ADMIN_USER_ID, subscription))
                .as(CANNOT_ADD_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(ADD_ERROR_LOG, ADMIN_USER_ID, SUBSCRIPTION_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID);
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanBulkDeleteTheirOwnSubscriptions(Roles role) {
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        subscription2.setUserId(USER_ID);
        subscription3.setUserId(USER_ID);

        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID2)).thenReturn(Optional.of(subscription2));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID3)).thenReturn(Optional.of(subscription3));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanBulkDeleteSubscriptions(
                USER_ID, List.of(SUBSCRIPTION_ID, SUBSCRIPTION_ID2, SUBSCRIPTION_ID3)
            ))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCannotBulkDeleteSubscriptionIfUserMisMatchedInSomeOfTheSubscriptions(Roles role) {
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        subscription2.setUserId(ANOTHER_USER_ID);
        subscription3.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(subscriptionRepository.findById(SUBSCRIPTION_ID)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.findById(SUBSCRIPTION_ID2)).thenReturn(Optional.of(subscription2));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanBulkDeleteSubscriptions(
                USER_ID, List.of(SUBSCRIPTION_ID, SUBSCRIPTION_ID2, SUBSCRIPTION_ID3)
            ))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(2);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(MISMATCH_ERROR_LOG, USER_ID, SUBSCRIPTION_ID2, ANOTHER_USER_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID3);
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        INTERNAL_SUPER_ADMIN_CTSC_ROLE,
        INTERNAL_SUPER_ADMIN_LOCAL_ROLE,
        INTERNAL_ADMIN_CTSC_ROLE,
        INTERNAL_ADMIN_LOCAL_ROLE,
        SYSTEM_ADMIN_ROLE
    }, mode = EnumSource.Mode.INCLUDE)
    void testNonVerifiedUsersCannotBulkDeleteSubscriptions(Roles role) {
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        subscription2.setUserId(USER_ID);
        subscription3.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanBulkDeleteSubscriptions(
                USER_ID, List.of(SUBSCRIPTION_ID, SUBSCRIPTION_ID2, SUBSCRIPTION_ID3)
            ))
                .as(CANNOT_DELETE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, USER_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID3);
        }
    }

    @Test
    void testUserCanDeleteSubscriptionReturnsTrueIfSubscriptionNotFound() {
        user.setRoles(SYSTEM_ADMIN);
        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(USER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteSubscriptions(USER_ID, SUBSCRIPTION_ID))
                .as(CAN_DELETE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanUpdateTheirOwnSubscriptions(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanUpdateSubscriptions(USER_ID, USER_ID))
                .as(CAN_UPDATE_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllVerifiedRoles")
    void testVerifiedUserCanNotUpdateOtherUserSubscriptions(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanUpdateSubscriptions(USER_ID, ANOTHER_USER_ID))
                .as(CANNOT_UPDATE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, USER_ID, ANOTHER_USER_ID));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        INTERNAL_SUPER_ADMIN_CTSC_ROLE,
        INTERNAL_SUPER_ADMIN_LOCAL_ROLE,
        INTERNAL_ADMIN_CTSC_ROLE,
        INTERNAL_ADMIN_LOCAL_ROLE,
        SYSTEM_ADMIN_ROLE
    }, mode = EnumSource.Mode.INCLUDE)
    void testNonVerifiedUsersCanNotUpdateTheirOwnSubscriptions(Roles role) {
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanUpdateSubscriptions(USER_ID, USER_ID))
                .as(CANNOT_UPDATE_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(UPDATE_ERROR_LOG, USER_ID, USER_ID));
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testSystemAdminUserCanDeleteLocationSubscriptionsForThirdPartyAccount(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);
        when(subscriptionRepository.findSubscriptionsByLocationId("123"))
            .thenReturn(List.of(subscription));

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteLocationSubscriptions(ADMIN_USER_ID, 123))
                .as(CAN_ADD_SUBSCRIPTION_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testSystemAdminUserCanNotDeleteLocationSubscriptionsForThirdPartyAccountWhenNotLoggedIn(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        subscription.setUserId(USER_ID);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteLocationSubscriptions(ADMIN_USER_ID, 123))
                .as(CANNOT_ADD_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(DELETE_ERROR_LOG, ADMIN_USER_ID));

            verify(subscriptionRepository, never()).findById(SUBSCRIPTION_ID);
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        "VERIFIED",
        INTERNAL_SUPER_ADMIN_CTSC_ROLE,
        INTERNAL_SUPER_ADMIN_LOCAL_ROLE,
        INTERNAL_ADMIN_CTSC_ROLE,
        INTERNAL_ADMIN_LOCAL_ROLE,
        "SYSTEM_ADMIN"
    }, mode = EnumSource.Mode.INCLUDE)
    void testSystemAdminUserCanNotDeleteLocationSubscriptionsForNonThirdPartyAccounts(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);
        when(subscriptionRepository.findSubscriptionsByLocationId("123"))
            .thenReturn(List.of(subscription));

        assertThat(subscriptionAuthorisationService.userCanDeleteLocationSubscriptions(ADMIN_USER_ID, 123))
            .as(CANNOT_ADD_SUBSCRIPTION_MESSAGE)
            .isFalse();

        verify(subscriptionRepository, never()).deleteByIdIn(List.of(subscription.getId()));
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testUserCanNotDeleteLocationSubscriptionsForThirdPartyAccountWhenNotSystemAdmin(Roles role) {
        adminUser.setRoles(INTERNAL_ADMIN_CTSC);
        user.setRoles(role);
        subscription.setUserId(USER_ID);
        when(authorisationCommonService.isAdmin()).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanDeleteLocationSubscriptions(ADMIN_USER_ID, 123))
                .as(CAN_ADD_SUBSCRIPTION_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            verify(subscriptionRepository, never()).deleteByIdIn(List.of(subscription.getId()));
        }
    }

    /// /////////////////////////////////////////////

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testSystemAdminUserCanGetSubscriptionChannelsForThirdPartyAccount(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanRetrieveChannels(ADMIN_USER_ID, USER_ID))
                .as(CAN_GET_CHANNELS_MESSAGE)
                .isTrue();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_EMPTY_MESSAGE)
                .isEmpty();
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testSystemAdminUserCanNotGetSubscriptionChannelsForThirdPartyAccountWhenNotLoggedIn(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanRetrieveChannels(ADMIN_USER_ID, USER_ID))
                .as(CANNOT_GET_CHANNELS_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(CHANNEL_ERROR_LOG, ADMIN_USER_ID));
        }
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {
        "VERIFIED",
        INTERNAL_SUPER_ADMIN_CTSC_ROLE,
        INTERNAL_SUPER_ADMIN_LOCAL_ROLE,
        INTERNAL_ADMIN_CTSC_ROLE,
        INTERNAL_ADMIN_LOCAL_ROLE,
        "SYSTEM_ADMIN"
    }, mode = EnumSource.Mode.INCLUDE)
    void testSystemAdminUserCanNotGetSubscriptionChannelsForNonThirdPartyAccounts(Roles role) {
        adminUser.setRoles(SYSTEM_ADMIN);
        user.setRoles(role);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        when(authorisationCommonService.isAdmin()).thenReturn(true);
        when(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID)).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanRetrieveChannels(ADMIN_USER_ID, USER_ID))
                .as(CANNOT_GET_CHANNELS_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(CHANNEL_ERROR_LOG, ADMIN_USER_ID));
        }
    }

    @ParameterizedTest
    @MethodSource("uk.gov.hmcts.reform.pip.model.account.Roles#getAllThirdPartyRoles")
    void testUserCanNotGetSubscriptionChannelsForThirdPartyAccountWhenNotSystemAdmin(Roles role) {
        adminUser.setRoles(INTERNAL_ADMIN_CTSC);
        user.setRoles(role);
        when(authorisationCommonService.isAdmin()).thenReturn(true);

        try (LogCaptor logCaptor = LogCaptor.forClass(SubscriptionAuthorisationService.class)) {
            assertThat(subscriptionAuthorisationService.userCanRetrieveChannels(ADMIN_USER_ID, USER_ID))
                .as(CANNOT_GET_CHANNELS_MESSAGE)
                .isFalse();

            assertThat(logCaptor.getErrorLogs())
                .as(LOG_NOT_EMPTY_MESSAGE)
                .hasSize(1);

            assertThat(logCaptor.getErrorLogs().getFirst())
                .as(LOG_MATCHED_MESSAGE)
                .contains(String.format(CHANNEL_ERROR_LOG, ADMIN_USER_ID));
        }
    }


}
