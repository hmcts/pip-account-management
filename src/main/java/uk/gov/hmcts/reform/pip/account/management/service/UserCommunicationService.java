package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserCommunicationRepository;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;

import java.util.List;

@Service
public class UserCommunicationService {
    private final UserCommunicationRepository userCommunicationRepository;

    @Autowired
    public UserCommunicationService(UserCommunicationRepository userCommunicationRepository) {
        this.userCommunicationRepository = userCommunicationRepository;
    }

    public List<UserCommunication> getAll() {
        return userCommunicationRepository.findAll();
    }
}
