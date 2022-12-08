package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Service class which handles dealing with views.
 */
@Service
@Slf4j
public class ViewService {

    @Autowired
    UserRepository userRepository;

    /**
     * Service method which refreshes the view.
     */
    public void refreshView() {
        log.info(writeLog("Refreshing Account view"));
        userRepository.refreshAccountView();
    }

}
