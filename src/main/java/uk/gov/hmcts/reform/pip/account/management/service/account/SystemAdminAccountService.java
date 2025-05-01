package uk.gov.hmcts.reform.pip.account.management.service.account;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SystemAdminAccountException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSystemAdminAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Set;

/**
 * Service class which deals with the creation of the System Admin accounts on the user table.
 */
@Service
@Slf4j
public class SystemAdminAccountService {
    private final Validator validator;
    private final UserRepository userRepository;
    private final Integer maxSystemAdminValue;

    @Autowired
    public SystemAdminAccountService(Validator validator, UserRepository userRepository,
                                     @Value("${admin.max-system-admin}")Integer maxSystemAdminValue) {
        this.validator = validator;
        this.userRepository = userRepository;
        this.maxSystemAdminValue = maxSystemAdminValue;
    }

    /**
     * This method deals with the creation of a system admin account.
     * @param account The system admin account to be created.
     * @return  The PiUser of the created system admin account.
     */
    public PiUser addSystemAdminAccount(SystemAdminAccount account) {
        validateSystemAdminAccount(account);
        return userRepository.save(account.convertToPiUser());

    }

    /**
     * A helper method which specifically handles validation failures on the system admin account.
     * @param account The system admin account to validate.
     */
    private void validateSystemAdminAccount(SystemAdminAccount account) {
        Set<ConstraintViolation<SystemAdminAccount>> constraintViolations = validator.validate(account);

        if (!constraintViolations.isEmpty()) {
            ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setErrorMessages(
                constraintViolations.stream()
                    .map(constraint -> constraint.getPropertyPath() + ": " + constraint.getMessage())
                    .toList()
            );
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }

        if (userRepository.findByEmailAndUserProvenance(account.getEmail(), UserProvenances.SSO).isPresent()) {
            ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setDuplicate(true);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }

        List<PiUser> systemAdminUsers = userRepository.findByRoles(Roles.SYSTEM_ADMIN)
            .stream()
            .filter(u -> UserProvenances.SSO.equals(u.getUserProvenance()))
            .toList();
        if (systemAdminUsers.size() >= maxSystemAdminValue) {
            ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setAboveMaxSystemAdmin(true);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }
    }
}
