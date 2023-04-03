package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("PMD.TooManyMethods")
class InactiveAccountManagementServiceTest {
    private static final UUID MEDIA_USER_UUID = UUID.randomUUID();
    private static final String MEDIA_USER_EMAIL = "media@test.com";
    private static final UUID AAD_ADMIN_UUID = UUID.randomUUID();
    private static final String AAD_ADMIN_USER_EMAIL = "aad_admin@test.com";
    private static final UUID CFT_IDAM_UUID = UUID.randomUUID();
    private static final String CFT_IDAM_USER_EMAIL = "cft_idam@test.com";
    private static final UUID CRIME_IDAM_UUID = UUID.randomUUID();
    private static final String CRIME_IDAM_USER_EMAIL = "crime_idam@test.com";
    private static final String AZURE_MEDIA_USER_NAME = "MediaUserName";
    private static final String AZURE_ADMIN_USER_NAME = "AdminUserName";
    private static final LocalDateTime LAST_SIGNED_IN_DATE = LocalDateTime.of(2022, 8, 1, 10, 0, 0);
    private static final String LAST_SIGNED_IN_DATE_STRING = "01 August 2022";
    private static final String FORENAME = "Test";
    private static final String SURNAME = "Surname";

    private static final PiUser MEDIA_USER = new PiUser(MEDIA_USER_UUID, UserProvenances.PI_AAD,
                                                        "1", MEDIA_USER_EMAIL, Roles.VERIFIED,
                                                        FORENAME, SURNAME, null, null, null);
    private static final PiUser AAD_ADMIN_USER = new PiUser(AAD_ADMIN_UUID, UserProvenances.PI_AAD,
                                                            "2", AAD_ADMIN_USER_EMAIL, Roles.INTERNAL_SUPER_ADMIN_CTSC,
                                                            FORENAME, SURNAME, null, null, LAST_SIGNED_IN_DATE);
    private static final PiUser CFT_IDAM_USER = new PiUser(CFT_IDAM_UUID, UserProvenances.CFT_IDAM,
                                                           "3", CFT_IDAM_USER_EMAIL, Roles.INTERNAL_ADMIN_CTSC,
                                                           FORENAME, SURNAME, null, null, LAST_SIGNED_IN_DATE);
    private static final PiUser CRIME_IDAM_USER = new PiUser(CRIME_IDAM_UUID, UserProvenances.CRIME_IDAM,
                                                             "4", CRIME_IDAM_USER_EMAIL, Roles.INTERNAL_ADMIN_CTSC,
                                                             FORENAME, SURNAME, null, null, LAST_SIGNED_IN_DATE);

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
    private InactiveAccountManagementService inactiveAccountManagementService;

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

        inactiveAccountManagementService.sendMediaUsersForVerification();
        verify(publicationService).sendAccountVerificationEmail(MEDIA_USER_EMAIL, AZURE_MEDIA_USER_NAME);
    }

    @Test
    void testNoMediaUsersForVerification() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList());

        inactiveAccountManagementService.sendMediaUsersForVerification();
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testNotifyAdminUsersToSignIn() throws AzureCustomException {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(AAD_ADMIN_USER));
        when(azureUserService.getUser(AAD_ADMIN_USER_EMAIL)).thenReturn(azureAdminUser);

        inactiveAccountManagementService.notifyAdminUsersToSignIn();
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(
            AAD_ADMIN_USER_EMAIL, AZURE_ADMIN_USER_NAME, UserProvenances.PI_AAD, LAST_SIGNED_IN_DATE_STRING
        );
    }

    @Test
    void testNoNotificationOfAdminUsersToSignIn() {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        inactiveAccountManagementService.notifyAdminUsersToSignIn();
        verifyNoInteractions(publicationService);
        verifyNoInteractions(azureUserService);
    }

    @Test
    void testNotifyIdamUsersToSignIn() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(List.of(CFT_IDAM_USER, CRIME_IDAM_USER));

        inactiveAccountManagementService.notifyIdamUsersToSignIn();
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(
            CFT_IDAM_USER_EMAIL, FORENAME + " " + SURNAME, UserProvenances.CFT_IDAM, LAST_SIGNED_IN_DATE_STRING
        );
        verify(publicationService).sendInactiveAccountSignInNotificationEmail(
            CRIME_IDAM_USER_EMAIL, FORENAME + " " + SURNAME, UserProvenances.CRIME_IDAM, LAST_SIGNED_IN_DATE_STRING
        );
    }

    @Test
    void testNoNotificationOfIdamUsersToSignIn() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        inactiveAccountManagementService.notifyIdamUsersToSignIn();
        verifyNoInteractions(publicationService);
    }

    @Test
    void testMediaAccountDeletion() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.singletonList(MEDIA_USER));

        inactiveAccountManagementService.findMediaAccountsForDeletion();
        verify(accountService).deleteAccount(MEDIA_USER_UUID);
    }

    @Test
    void testNoMediaAccountDeletion() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList());

        inactiveAccountManagementService.findMediaAccountsForDeletion();
        verifyNoInteractions(accountService);
    }

    @Test
    void testAdminAccountDeletion() {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.singletonList(AAD_ADMIN_USER));

        inactiveAccountManagementService.findAdminAccountsForDeletion();
        verify(accountService).deleteAccount(AAD_ADMIN_UUID);
    }

    @Test
    void testNoAdminAccountDeletion() {
        when(userRepository.findAadAdminUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        inactiveAccountManagementService.findAdminAccountsForDeletion();
        verifyNoInteractions(accountService);
    }

    @Test
    void testIdamAccountDeletion() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(List.of(CFT_IDAM_USER, CRIME_IDAM_USER));

        inactiveAccountManagementService.findIdamAccountsForDeletion();
        verify(accountService).deleteAccount(CFT_IDAM_UUID);
        verify(accountService).deleteAccount(CRIME_IDAM_UUID);
    }

    @Test
    void testNoIdamAccountDeletion() {
        when(userRepository.findIdamUsersByLastSignedInDate(anyInt()))
            .thenReturn(Collections.emptyList());

        inactiveAccountManagementService.findIdamAccountsForDeletion();
        verifyNoInteractions(accountService);
    }
}
