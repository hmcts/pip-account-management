package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import javax.validation.Validator;

/**
 * Service layer that deals with the creation of applications.
 * The storage mechanism (e.g Azure) is seperated into a seperate class.
 */
@Component
public class ApplicationService {

    @Autowired
    Validator validator;



}
