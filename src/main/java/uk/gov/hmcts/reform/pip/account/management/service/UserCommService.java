package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserCommRepository;
import uk.gov.hmcts.reform.pip.account.management.model.UserComm;

import java.util.List;

@Service
public class UserCommService {
    private final UserCommRepository userCommRepository;

    @Autowired
    public UserCommService(UserCommRepository userCommRepository) {
        this.userCommRepository = userCommRepository;
    }

    public List<UserComm> getAll() {
        return userCommRepository.findAll();
    }
}
