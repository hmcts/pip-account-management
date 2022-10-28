package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@SuppressWarnings("PMD.TooManyMethods")
class AccountVerificationServiceTest {
    private static final String MEDIA_USER_EMAIL = "media@test.com";
    private static final String AAD_ADMIN_USER_EMAIL = "aad_admin@test.com";
    private static final String CFT_IDAM_USER_EMAIL = "cft_idam@test.com";
    private static final String CRIME_IDAM_USER_EMAIL = "crime_idam@test.com";
    private static final String AZURE_MEDIA_USER_NAME = "MediaUserName";
    private static final String AZURE_ADMIN_USER_NAME = "AdminUserName";
    private static final String IDAM_USER_NAME = "IdamUserName";
    private static final LocalDateTime LAST_SIGNED_IN_DATE = LocalDateTime.of(2022, 8, 1, 10, 0, 0);
    private static final String LAST_SIGNED_IN_DATE_STRING = "01 August 2022";

    private static final PiUser MEDIA_USER = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD,
                                                        "1", MEDIA_USER_EMAIL, Roles.VERIFIED,
                                                        null, null, null);
    private static final PiUser AAD_ADMIN_USER = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD,
                                                            "2", AAD_ADMIN_USER_EMAIL, Roles.INTERNAL_SUPER_ADMIN_CTSC,
                                                            null, null, LAST_SIGNED_IN_DATE);
    private static final PiUser CFT_IDAM_USER = new PiUser(UUID.randomUUID(), UserProvenances.CFT_IDAM,
                                                           "3", CFT_IDAM_USER_EMAIL, Roles.INTERNAL_ADMIN_CTSC,
                                                           null, null, LAST_SIGNED_IN_DATE);
    private static final PiUser CRIME_IDAM_USER = new PiUser(UUID.randomUUID(), UserProvenances.CFT_IDAM,
                                                             "4", CRIME_IDAM_USER_EMAIL, Roles.INTERNAL_ADMIN_CTSC,
                                                             null, null, LAST_SIGNED_IN_DATE);

    private static User azureMediaUser = new User();
    private static User azureAdminUser = new User();

    @Mock
    UserRepository userRepository;

    @Mock
    AzureUserService azureUserService;

    @Mock
    AccountService accountService;

    @Mock
    PublicationService publicationService;

    @InjectMocks
    private AccountVerificationService accountVerificationService;

    @BeforeAll
    static void setup() {
        azureMediaUser.givenName = AZURE_MEDIA_USER_NAME;
        azureAdminUser.givenName = AZURE_ADMIN_USER_NAME;
    }

    @Test
    void testSendMediaUsersForVerification() throws AzureCustomException {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.singletonList(MEDIA_USER));
        when(azureUserService.getUser(MEDIA_USER_EMAIL)).thenReturn(azureMediaUser);

        accountVerificationService.sendMediaUsersForVerification();
        verify(publicationService).sendAccountVerificationEmail(MEDIA_USER_EMAIL, AZURE_MEDIA_USER_NAME);
    }

    @Test
    void testNoMediaUsersForVerification() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.sendMediaUsersForVerification();
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testNotifyAdminUsersToSignIn() throws AzureCustomException {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(AAD_ADMIN_USER));
        when(azureUserService.getUser(AAD_ADMIN_USER_EMAIL)).thenReturn(azureAdminUser);

        accountVerificationService.notifyAdminUsersToSignIn();
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(
            AAD_ADMIN_USER_EMAIL, AZURE_ADMIN_USER_NAME, LAST_SIGNED_IN_DATE_STRING
        );
    }

    @Test
    void testNoNotificationOfAdminUsersToSignIn() {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.notifyAdminUsersToSignIn();
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testNotifyIdamUsersToSignIn() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(List.of(CFT_IDAM_USER, CRIME_IDAM_USER));

        accountVerificationService.notifyIdamUsersToSignIn();
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(
            CFT_IDAM_USER_EMAIL, IDAM_USER_NAME, LAST_SIGNED_IN_DATE_STRING
        );
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(
            CRIME_IDAM_USER_EMAIL, IDAM_USER_NAME, LAST_SIGNED_IN_DATE_STRING
        );
    }

    @Test
    void testNoNotificationOfIdamUsersToSignIn() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.notifyIdamUsersToSignIn();
        verifyNoInteractions(publicationService);
    }

    @Test
    void testMediaAccountDeletion() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.singletonList(MEDIA_USER));

        accountVerificationService.findMediaAccountsForDeletion();
        verify(accountService).deleteAccount(MEDIA_USER_EMAIL, true);
    }

    @Test
    void testNoMediaAccountDeletion() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.findMediaAccountsForDeletion();
        verifyNoInteractions(accountService);
    }

    @Test
    void testAdminAccountDeletion() {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(AAD_ADMIN_USER));

        accountVerificationService.findAdminAccountsForDeletion();
        verify(accountService).deleteAccount(AAD_ADMIN_USER_EMAIL, true);
    }

    @Test
    void testNoAdminAccountDeletion() {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.findAdminAccountsForDeletion();
        verifyNoInteractions(accountService);
    }

    @Test
    void testIdamAccountDeletion() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(List.of(CFT_IDAM_USER, CRIME_IDAM_USER));

        accountVerificationService.findIdamAccountsForDeletion();
        verify(accountService).deleteAccount(CFT_IDAM_USER_EMAIL, false);
        verify(accountService).deleteAccount(CRIME_IDAM_USER_EMAIL, false);
    }

    @Test
    void testNoIdamAccountDeletion() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.findIdamAccountsForDeletion();
        verifyNoInteractions(accountService);
    }
}
