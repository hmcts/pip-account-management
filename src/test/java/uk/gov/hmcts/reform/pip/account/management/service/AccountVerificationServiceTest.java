package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import nl.altindag.log.LogCaptor;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    void testNoAccountDeletionAndNotification() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList());
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.processEligibleUsersForVerification();

        verifyNoInteractions(accountService);
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testAccountDeletionOfMediaUserOnly() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.singletonList(MEDIA_USER))
            .thenReturn(Collections.emptyList());
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.processEligibleUsersForVerification();

        verify(accountService).deleteAccount(MEDIA_USER_EMAIL, true);
        verifyNoMoreInteractions(accountService);
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testAccountDeletionOfAadAdminAndIdamUsersOnly() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList());
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(AAD_ADMIN_USER))
            .thenReturn(Collections.emptyList());
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(CRIME_IDAM_USER))
            .thenReturn(Collections.emptyList());

        accountVerificationService.processEligibleUsersForVerification();

        verify(accountService).deleteAccount(AAD_ADMIN_USER_EMAIL, true);
        verify(accountService).deleteAccount(CRIME_IDAM_USER_EMAIL, false);
        verifyNoMoreInteractions(accountService);
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testAccountDeletionOfAllUsers() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.singletonList(MEDIA_USER))
            .thenReturn(Collections.emptyList());
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(AAD_ADMIN_USER))
            .thenReturn(Collections.emptyList());
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Arrays.asList(CFT_IDAM_USER, CRIME_IDAM_USER))
            .thenReturn(Collections.emptyList());

        accountVerificationService.processEligibleUsersForVerification();

        verify(accountService).deleteAccount(MEDIA_USER_EMAIL, true);
        verify(accountService).deleteAccount(AAD_ADMIN_USER_EMAIL, true);
        verify(accountService).deleteAccount(CFT_IDAM_USER_EMAIL, false);
        verify(accountService).deleteAccount(CRIME_IDAM_USER_EMAIL, false);
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testAccountDeletionOfCftIdamUserAndNotificationOfMediaUser() throws AzureCustomException {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList())
            .thenReturn(Collections.singletonList(MEDIA_USER));
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(CFT_IDAM_USER))
            .thenReturn(Collections.emptyList());
        when(azureUserService.getUser(MEDIA_USER_EMAIL)).thenReturn(azureMediaUser);

        accountVerificationService.processEligibleUsersForVerification();

        verify(accountService).deleteAccount(CFT_IDAM_USER_EMAIL, false);
        verifyNoMoreInteractions(accountService);
        verify(publicationService).sendAccountVerificationEmail(MEDIA_USER_EMAIL, AZURE_MEDIA_USER_NAME);
    }

    @Test
    void testNotificationOfAllUsers() throws AzureCustomException {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList())
            .thenReturn(Collections.singletonList(MEDIA_USER));
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList())
            .thenReturn(Collections.singletonList(AAD_ADMIN_USER));
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList())
            .thenReturn(Arrays.asList(CFT_IDAM_USER, CRIME_IDAM_USER));
        when(azureUserService.getUser(MEDIA_USER_EMAIL)).thenReturn(azureMediaUser);
        when(azureUserService.getUser(AAD_ADMIN_USER_EMAIL)).thenReturn(azureAdminUser);

        accountVerificationService.processEligibleUsersForVerification();

        verifyNoInteractions(accountService);
        verify(publicationService).sendAccountVerificationEmail(MEDIA_USER_EMAIL, AZURE_MEDIA_USER_NAME);
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(AAD_ADMIN_USER_EMAIL,
                                                                              AZURE_ADMIN_USER_NAME,
                                                                              LAST_SIGNED_IN_DATE_STRING);
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(CFT_IDAM_USER_EMAIL,
                                                                              IDAM_USER_NAME,
                                                                              LAST_SIGNED_IN_DATE_STRING);
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(CRIME_IDAM_USER_EMAIL,
                                                                              IDAM_USER_NAME,
                                                                              LAST_SIGNED_IN_DATE_STRING);
    }

    @Test
    void testMediaUserNotificationWithAzureException() throws AzureCustomException {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList())
            .thenReturn(Collections.singletonList(MEDIA_USER));
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());
        when(azureUserService.getUser(MEDIA_USER_EMAIL)).thenThrow(new AzureCustomException("error"));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountVerificationService.class)) {
            accountVerificationService.processEligibleUsersForVerification();
            assertThat(logCaptor.getErrorLogs())
                .as("Incorrect error message")
                .hasSize(1)
                .first()
                .asString()
                .contains("Error when getting user from azure");
        }

        verifyNoInteractions(accountService);
        verifyNoInteractions(publicationService);
    }
}
