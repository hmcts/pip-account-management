package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

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
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.Roles.VERIFIED;

@SuppressWarnings("PMD.UnitTestAssertionsShouldIncludeMessage")
@ExtendWith(MockitoExtension.class)
class AuthorisationCommonServiceTest {

    private static PiUser user = new PiUser();
    private static PiUser adminUser = new PiUser();
    private static Subscription subscription = new Subscription();
    private static Subscription subscription2 = new Subscription();
    private static Subscription subscription3 = new Subscription();

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID ADMIN_USER_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID2 = UUID.randomUUID();
    private static final UUID SUBSCRIPTION_ID3 = UUID.randomUUID();

    private static final String ADMIN_ROLE = "APPROLE_api.request.admin";
    private static final String UNKNOWN_ROLE = "APPROLE_api.request.unknown";
    private static final String TEST_USER_ID = "123";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AuthorisationCommonService authorisationCommonService;

    @Mock
    private SecurityContext securityContext;

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

    protected void setupWithAuth() {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(ADMIN_ROLE)
        );

        Authentication auth = new TestingAuthenticationToken(TEST_USER_ID, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
    }

    protected void setupWithoutAuth() {
        List<GrantedAuthority> authorities = List.of(
            new SimpleGrantedAuthority(UNKNOWN_ROLE)
        );

        Authentication auth = new TestingAuthenticationToken(TEST_USER_ID, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(auth);
        lenient().when(securityContext.getAuthentication()).thenReturn(auth);
    }

    @Test
    void testUserIsSystemAdminAndLoggedIn() {
        setupWithAuth();
        adminUser.setRoles(SYSTEM_ADMIN);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        assertTrue(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID));
        assertTrue(authorisationCommonService.hasOAuthAdminRole());
    }

    @Test
    void testUserIsSystemAdminNotLoggedIn() {
        setupWithoutAuth();
        adminUser.setRoles(SYSTEM_ADMIN);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        assertTrue(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID));
        assertFalse(authorisationCommonService.hasOAuthAdminRole());
    }

    @Test
    void testUserIsOtherAdminAndLoggedIn() {
        setupWithAuth();
        adminUser.setRoles(INTERNAL_ADMIN_CTSC);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        assertFalse(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID));
        assertTrue(authorisationCommonService.hasOAuthAdminRole());
    }

    @Test
    void testUserIsOtherRoleAndNotLoggedIn() {
        setupWithoutAuth();
        adminUser.setRoles(VERIFIED);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        assertFalse(authorisationCommonService.hasOAuthAdminRole());
        assertFalse(authorisationCommonService.isSystemAdmin(ADMIN_USER_ID));
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class,
        names = { "INTERNAL_SUPER_ADMIN_CTSC",
            "INTERNAL_SUPER_ADMIN_LOCAL",
            "INTERNAL_ADMIN_CTSC",
            "INTERNAL_ADMIN_LOCAL",
            "SYSTEM_ADMIN" },
        mode = EnumSource.Mode.INCLUDE)
    void testUserIsAdminAndLoggedIn(Roles role) {
        setupWithAuth();
        adminUser.setRoles(role);
        when(accountService.getUserById(ADMIN_USER_ID)).thenReturn(adminUser);

        assertTrue(authorisationCommonService.isUserAdmin(ADMIN_USER_ID));
        assertTrue(authorisationCommonService.hasOAuthAdminRole());
    }

    @Test
    void testIsUserVerified() {
        user.setRoles(VERIFIED);
        when(accountService.getUserById(USER_ID)).thenReturn(user);
        assertTrue(authorisationCommonService.isUserVerified(USER_ID));
    }

    @Test
    void testIsUserCanVerifiedItSelfOnly() {
        PiUser randomUser = new PiUser();
        randomUser.setRoles(VERIFIED);
        randomUser.setUserId(UUID.randomUUID());
        when(accountService.getUserById(USER_ID)).thenReturn(randomUser);
        assertFalse(authorisationCommonService.isUserVerified(USER_ID));
    }
}
