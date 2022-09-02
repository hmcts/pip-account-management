package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import nl.altindag.log.LogCaptor;
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

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AccountVerificationServiceTest {
    private static final String MEDIA_USER_EMAIL = "media@test.com";
    private static final String AZURE_MEDIA_USER_NAME = "MediaUserName";

    private static final PiUser MEDIA_USER = new PiUser(UUID.randomUUID(), UserProvenances.PI_AAD,
                                                        "1", MEDIA_USER_EMAIL, Roles.VERIFIED,
                                                        null, null);

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

    @Test
    void testMediaAccountDeletion() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.singletonList(MEDIA_USER));

        accountVerificationService.findMediaAccountsForDeletion();
        verify(accountService).deleteAccount(MEDIA_USER_EMAIL);
    }

    @Test
    void testNoMediaAccountForDeletion() {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.emptyList());

        accountVerificationService.findMediaAccountsForDeletion();
        verifyNoInteractions(accountService);
    }


    @Test
    void testSendMediaUsersForVerification() throws AzureCustomException {
        User azureMediaUser = new User();
        azureMediaUser.givenName = AZURE_MEDIA_USER_NAME;

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
    void testMediaUserNotificationWithAzureException() throws AzureCustomException {
        when(userRepository.findVerifiedUsersByLastVerifiedDate(anyInt()))
            .thenReturn(Collections.singletonList(MEDIA_USER));
        when(azureUserService.getUser(MEDIA_USER_EMAIL)).thenThrow(new AzureCustomException("error"));

        try (LogCaptor logCaptor = LogCaptor.forClass(AccountVerificationService.class)) {
            accountVerificationService.sendMediaUsersForVerification();
            assertThat(logCaptor.getErrorLogs())
                .as("Incorrect error message")
                .hasSize(1)
                .first()
                .asString()
                .contains("Error when getting user from azure");
        }

        verifyNoInteractions(publicationService);
    }
}
