package uk.gov.hmcts.reform.pip.account.management.service;

import javassist.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.model.CftUser;
import uk.gov.hmcts.reform.pip.account.management.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Slf4j
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserRepository repository;

    @Override
    public CftUser createUser(CftUser cftUser) {
        return repository.save(cftUser);
    }

    @Override
    public void deleteById(UUID id) throws NotFoundException {
        Optional<CftUser> user = repository.findById(id);
        if(user.isEmpty()) {
            throw new NotFoundException(String.format("No user found with the uuid %s", id));
        }
        repository.deleteById(id);
    }

    @Override
    public List<CftUser> findAll() {
        return repository.findAll();
    }

    @Override
    public CftUser findById(UUID id) throws NotFoundException {
       Optional<CftUser> user = repository.findById(id);
       if (user.isEmpty()) {
           throw new NotFoundException(String.format("No user found with the uuid %s", id));
       }
       return user.get();
    }
}
