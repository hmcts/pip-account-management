package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Service class which handles dealing with views.
 */
@Service
@Slf4j
public class ViewService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Autowired
    public ViewService(UserRepository userRepository, SubscriptionRepository subscriptionRepository) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
    }

    /**
     * Service method which refreshes the view.
     */
    public void refreshView() {
        log.info(writeLog("Refreshing Account and Subscription views"));
        userRepository.refreshAccountView();
        subscriptionRepository.refreshSubscriptionView();
    }

}
