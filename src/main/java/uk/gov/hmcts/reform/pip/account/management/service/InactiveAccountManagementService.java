package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.service.helpers.DateTimeHelper;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Slf4j
@Service
public class InactiveAccountManagementService {

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
    public InactiveAccountManagementService(UserRepository userRepository, AzureUserService azureUserService,
                                            PublicationService publicationService, AccountService accountService) {
        this.userRepository = userRepository;
        this.azureUserService = azureUserService;
        this.publicationService = publicationService;
        this.accountService = accountService;
    }

    /**
     * Method that gets all media users who last verified at least 350 days ago.
     * Then send their details on to publication services to send them a verification email.
     */
    public void sendMediaUsersForVerification() {
        userRepository.findVerifiedUsersByLastVerifiedDate(mediaAccountVerificationDays).forEach(user -> {
            try {
                publicationService.sendAccountVerificationEmail(
                    user.getEmail(),
                    azureUserService.getUser(user.getEmail()).givenName
                );
            } catch (AzureCustomException ex) {
                log.error(writeLog("Error when getting user from azure: " + ex.getMessage()));
            }
        });
    }

    /**
     * Method that gets all admin users who last signed in at least 76 days ago.
     * Then send their details on to publication services to send them a notification email.
     */
    public void notifyAdminUsersToSignIn() {
        userRepository.findAadAdminUsersByLastSignedInDate(aadAdminAccountSignInNotificationDays)
            .forEach(user -> {
                try {
                    publicationService.sendInactiveAccountSignInNotificationEmail(
                        user.getEmail(),
                        azureUserService.getUser(user.getEmail()).givenName,
                        user.getUserProvenance(),
                        DateTimeHelper.localDateTimeToDateString(user.getLastSignedInDate())
                    );
                } catch (AzureCustomException ex) {
                    log.error(writeLog("Error when getting user from azure: " + ex.getMessage()));
                }
            });
    }

    /**
     * Method that gets all idam users who last signed in at least 118 days ago.
     * Then send their details on to publication services to send them a notification email.
     */
    public void notifyIdamUsersToSignIn() {
        userRepository.findIdamUsersByLastSignedInDate(idamAccountSignInNotificationDays)
            .forEach(user -> publicationService.sendInactiveAccountSignInNotificationEmail(
                user.getEmail(),
                user.getForenames() + " " + user.getSurname(),
                user.getUserProvenance(),
                DateTimeHelper.localDateTimeToDateString(user.getLastSignedInDate())
            ));
    }

    /**
     * Method that gets all media users who have not verified their account (default to 365 days).
     * Account service handles the deletion of their AAD, P&I user and subscriptions.
     */
    public void findMediaAccountsForDeletion() {
        userRepository.findVerifiedUsersByLastVerifiedDate(mediaAccountDeletionDays)
            .forEach(user -> accountService.deleteAccount(user.getUserId()));
    }

    /**
     * Method that gets all admin users who have not signed in to their account (default to 90 days).
     * Account service handles the deletion of their AAD, P&I user and subscriptions.
     */
    public void findAdminAccountsForDeletion() {
        userRepository.findAadAdminUsersByLastSignedInDate(aadAdminAccountDeletionDays)
            .forEach(user -> accountService.deleteAccount(user.getUserId()));
    }

    /**
     * Method that gets all idam users who have not signed in their account (default to 132 days).
     * Account service handles the deletion of their P&I user and subscriptions.
     */
    public void findIdamAccountsForDeletion() {
        userRepository.findIdamUsersByLastSignedInDate(idamAccountDeletionDays)
            .forEach(user -> accountService.deleteAccount(user.getUserId()));
    }
}

