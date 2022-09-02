package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;

@Slf4j
@Service
public class AccountVerificationService {

    private final UserRepository userRepository;
    private final AzureUserService azureUserService;
    private final PublicationService publicationService;
    private final AccountService accountService;

    @Value("${verification.media-account-verification-email-days}")
    private int mediaAccountVerificationDays;

    @Value("${verification.media-account-deletion-days}")
    private int mediaAccountDeletionDays;

    @Autowired
    public AccountVerificationService(UserRepository userRepository, AzureUserService azureUserService,
                                      PublicationService publicationService, AccountService accountService) {
        this.userRepository = userRepository;
        this.azureUserService = azureUserService;
        this.publicationService = publicationService;
        this.accountService = accountService;
    }

    /**
     * Scheduled job that handles the media email verification flow.
     */
    @Scheduled(cron = "${cron.media-account-verification-check}")
    public void processEligibleMediaUsersForVerification() {
        findMediaAccountsForDeletion();
        sendMediaUsersForVerification();
    }


    /**
     * Method that gets all media users who last verified at least 350 days ago.
     * Then send their details on to publication services to send them a verification email.
     */
    public void sendMediaUsersForVerification() {
        userRepository.findVerifiedUsersByLastVerifiedDate(mediaAccountVerificationDays).forEach(user -> {
            try {
                log.info(publicationService.sendAccountVerificationEmail(
                    user.getEmail(),
                    azureUserService.getUser(user.getEmail()).givenName
                ));
            } catch (AzureCustomException ex) {
                log.error("Error when getting user from azure: %s", ex.getMessage());
            }
        });
    }

    /**
     * Method that gets all media users who have not verified their account in 365 days.
     * Account service handles the deletion of their AAD, P&I user and subscriptions.
     */
    public void findMediaAccountsForDeletion() {
        userRepository.findVerifiedUsersByLastVerifiedDate(mediaAccountDeletionDays).forEach(user -> {
            log.info(accountService.deleteAccount(user.getEmail()));
        });
    }
}

