package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.service.helpers.DateTimeHelper;

import java.util.Collection;
import java.util.stream.Stream;

import static uk.gov.hmcts.reform.pip.account.management.model.UserProvenances.PI_AAD;
import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Slf4j
@Service
public class AccountVerificationService {
    // TODO: Needs to be replaced with actual user name from IDAM after it is implemented
    private static final String PLACEHOLDER_IDAM_USER_NAME = "IdamUserName";

    private final UserRepository userRepository;
    private final AzureUserService azureUserService;
    private final PublicationService publicationService;
    private final AccountService accountService;

    @Value("${verification.media-account-verification-email-days}")
    private int mediaAccountVerificationDays;

    @Value("${verification.media-account-deletion-days}")
    private int mediaAccountDeletionDays;

    @Value("${verification.aad-admin-account-sign-in-notification-days}")
    private int aadAdminAccountSignInNotificationDays;

    @Value("${verification.aad-admin-account-deletion-days}")
    private int aadAdminAccountDeletionDays;

    @Value("${verification.idam-account-sign-in-notification-days}")
    private int idamAccountSignInNotificationDays;

    @Value("${verification.idam-account-deletion-days}")
    private int idamAccountDeletionDays;

    @Autowired
    public AccountVerificationService(UserRepository userRepository, AzureUserService azureUserService,
                                      PublicationService publicationService, AccountService accountService) {
        this.userRepository = userRepository;
        this.azureUserService = azureUserService;
        this.publicationService = publicationService;
        this.accountService = accountService;
    }

    /**
     * Scheduled job that handles the email verification and sign in notification flow.
     */
    @Scheduled(cron = "${cron.account-verification-check}")
    public void processEligibleUsersForVerification() {
        findAccountsForDeletion();
        sendMediaUsersForVerification();
        notifyAadAdminAndIdamUsersToSignIn();
    }

    /**
     * Method that gets all media users who last verified at least 350 days ago.
     * Then send their details on to publication services to send them a verification email.
     */
    private void sendMediaUsersForVerification() {
        userRepository.findVerifiedUsersByLastVerifiedDate(mediaAccountVerificationDays).forEach(user -> {
            try {
                log.info(writeLog(publicationService.sendAccountVerificationEmail(
                    user.getEmail(),
                    azureUserService.getUser(user.getEmail()).givenName
                )));
            } catch (AzureCustomException ex) {
                log.error(writeLog("Error when getting user from azure: " + ex.getMessage()));
            }
        });
    }

    /**
     * Method that gets all AAD amin users who last signed in at least 76 days (by default) ago, and gets all
     * IDAM users who last signed in at least 118 days (by default) ago.
     * Then send their details on to publication services to send them a notification email.
     */
    private void notifyAadAdminAndIdamUsersToSignIn() {
        Stream.of(
                userRepository.findAadAdminUsersByLastSignedInDate(aadAdminAccountSignInNotificationDays),
                userRepository.findIdamUsersByLastSignedInDate(idamAccountSignInNotificationDays))
            .flatMap(Collection::stream)
            .forEach(user -> {
                try {
                    String name = PI_AAD.equals(user.getUserProvenance())
                        ? azureUserService.getUser(user.getEmail()).givenName
                        : PLACEHOLDER_IDAM_USER_NAME;
                    log.info(writeLog(publicationService.sendInactiveAccountSignInNotificationEmail(
                        user.getEmail(),
                        name,
                        DateTimeHelper.localDateTimeToDateString(user.getLastSignedInDate()
                    ))));
                } catch (AzureCustomException ex) {
                    log.error(writeLog("Error when getting user from azure: " + ex.getMessage()));
                }
            });
    }

    /**
     * Method that gets all media users who have not verified their account (default to 365 days), and AAD admin and
     * IDAM users who have not signed in to their account (default to 90 days for AAD admin and 128 days for IDAM).
     * Account service handles the deletion of their AAD, P&I user and subscriptions.
     */
    private void findAccountsForDeletion() {
        Stream.of(
                userRepository.findVerifiedUsersByLastVerifiedDate(mediaAccountDeletionDays),
                userRepository.findAadAdminUsersByLastSignedInDate(aadAdminAccountDeletionDays),
                userRepository.findIdamUsersByLastSignedInDate(idamAccountDeletionDays))
            .flatMap(Collection::stream)
            .forEach(
                user -> log.info(writeLog(accountService.deleteAccount(user.getEmail(),
                                                              PI_AAD.equals(user.getUserProvenance()))))
            );
    }
}

