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

    @Value("${verification.media-account-verification-email-days}")
    private int mediaAccountVerificationDays;


    @Autowired
    public AccountVerificationService(UserRepository userRepository, AzureUserService azureUserService,
                                      PublicationService publicationService) {
        this.userRepository = userRepository;
        this.azureUserService = azureUserService;
        this.publicationService = publicationService;
    }

    /**
     * Scheduled job that collects eligible media users for email verification.
     */
    @Scheduled(cron = "${cron.media-account-verification-check}")
    public void processEligibleMediaUsersForVerification() {
        sendMediaUsersForVerification();
    }


    /**
     * Method that gets all media users who last verified at least 350 days ago.
     * Then send their details on to publication services to send them a verification email.
     */
    private void sendMediaUsersForVerification() {
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
}

