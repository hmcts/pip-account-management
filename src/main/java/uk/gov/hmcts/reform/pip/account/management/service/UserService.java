package uk.gov.hmcts.reform.pip.account.management.service;


import javassist.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.CftUser;

import java.util.List;
import java.util.UUID;

public interface UserService {

    CftUser createUser(CftUser cftUser);

    void deleteById(UUID id) throws NotFoundException;

    List<CftUser> findAll();

    CftUser findById(UUID id) throws NotFoundException;


}
