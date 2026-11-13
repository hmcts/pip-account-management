package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserCommunicationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;

import java.util.List;
import java.util.UUID;

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

    public UserCommunication findUserCommunicationById(UUID id) {
        return userCommunicationRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(
                String.format("User communication with ID %s could not be found", id)
            ));
    }

    public UserCommunication createUserCommunication(UserCommunication userCommunication) {
        return userCommunicationRepository.save(userCommunication);
    }

    public UserCommunication updateUserCommunication(UserCommunication userCommunication, UUID id) {
        findUserCommunicationById(id);
        return userCommunicationRepository.save(userCommunication);
    }

    public void deleteUserCommunication(UUID id) {
        findUserCommunicationById(id);
        userCommunicationRepository.deleteById(id);
    }
}
