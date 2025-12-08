package uk.gov.hmcts.reform.pip.account.management.service.account;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
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

    @Value("${verification.aad-admin-account-deletion-days}")
    private int aadAdminAccountDeletionDays;

    @Value("${verification.sso-admin-account-deletion-days}")
    private int ssoAdminAccountDeletionDays;

    @Value("${verification.cft-idam-account-sign-in-notification-days}")
    private int cftIdamAccountSignInNotificationDays;

    @Value("${verification.cft-idam-account-deletion-days}")
    private int cftIdamAccountDeletionDays;

    @Value("${verification.crime-idam-account-sign-in-notification-days}")
    private int crimeIdamAccountSignInNotificationDays;

    @Value("${verification.crime-idam-account-deletion-days}")
    private int crimeIdamAccountDeletionDays;

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
        userRepository.findVerifiedUsersForNotificationByLastVerifiedDate(mediaAccountVerificationDays)
            .forEach(user -> {
                try {
                    publicationService.sendAccountVerificationEmail(
                        user.getEmail(),
                        azureUserService.getUser(user.getEmail()).getGivenName()
                    );
                } catch (AzureCustomException ex) {
                    log.error(writeLog("Error when getting user from azure: " + ex.getMessage()));
                }
            });
    }

    /**
     * Method that gets all idam users who last signed in at least 118 days ago for cft
     * and 180 days for crime.
     * Then send their details on to publication services to send them a notification email.
     */
    public void notifyIdamUsersToSignIn() {
        userRepository.findIdamUsersForNotificationByLastSignedInDate(cftIdamAccountSignInNotificationDays,
                                                                      crimeIdamAccountSignInNotificationDays)
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
        userRepository.findVerifiedUsersForDeletionByLastVerifiedDate(mediaAccountDeletionDays)
            .forEach(user -> accountService.deleteAccount(user.getUserId()));
    }

    /**
     * Method that gets all admin users who have not signed in to their account (default to 90 days).
     * Account service handles the deletion of their AAD, P&I user and subscriptions.
     */
    public void findAdminAccountsForDeletion() {
        userRepository.findAdminUsersForDeletionByLastSignedInDate(aadAdminAccountDeletionDays,
                                                                   ssoAdminAccountDeletionDays)
            .forEach(user -> accountService.deleteAccount(user.getUserId()));
    }

    /**
     * Method that gets all idam users who have not signed in their account (cft to 132
     * and crime to 208 days)
     * Account service handles the deletion of their P&I user and subscriptions.
     */
    public void findIdamAccountsForDeletion() {
        userRepository.findIdamUsersForDeletionByLastSignedInDate(cftIdamAccountDeletionDays,
                                                                  crimeIdamAccountDeletionDays)
            .forEach(user -> accountService.deleteAccount(user.getUserId()));
    }
}

