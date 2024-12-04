package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SystemAdminAccountException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSystemAdminAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.CreateSystemAdminAction;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Service class which deals with the creation of the System Admin accounts. A seperate class has been created due to
 * the extra constraints and complexities of logging System Admin accounts.
 */
@Service
@Slf4j
public class SystemAdminB2CAccountService {


    private final Validator validator;
    private final AzureUserService azureUserService;
    private final UserRepository userRepository;
    private final PublicationService publicationService;
    private final AccountService accountService;
    private final Integer maxSystemAdminValue;

    @Autowired
    public SystemAdminB2CAccountService(Validator validator, AzureUserService azureUserService,
                                        UserRepository userRepository, PublicationService publicationService,
                                        @Value("${admin.max-system-admin}")Integer maxSystemAdminValue,
                                        AccountService accountService) {
        this.validator = validator;
        this.azureUserService = azureUserService;
        this.userRepository = userRepository;
        this.publicationService = publicationService;
        this.maxSystemAdminValue = maxSystemAdminValue;
        this.accountService = accountService;
    }

    /**
     * This method deals with the creation of a system admin account.
     * @param account The system admin account to be created.
     * @param issuerId The ID of the user creating the account.
     * @return  The PiUser of the created system admin account.
     */
    public PiUser addSystemAdminAccount(SystemAdminAccount account, String issuerId) {
        PiUser piUser = accountService.getUserById(UUID.fromString(issuerId));
        validateSystemAdminAccount(account, issuerId, piUser.getEmail());
        try {
            User user = azureUserService.createUser(account.convertToAzureAccount(), false);
            PiUser createdUser = userRepository.save(account.convertToPiUser(user.getId()));
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.SUCCEEDED, piUser.getEmail());

            publicationService.sendNotificationEmail(
                account.getEmail(),
                account.getFirstName(),
                account.getSurname()
            );
            return createdUser;
        } catch (AzureCustomException e) {
            ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setErrorMessages(List.of(e.getLocalizedMessage()));
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.FAILED, piUser.getEmail());
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }

    }

    /**
     * This method handles the logging and publishing that a new system admin account has been created.
     * @param systemAdminAccount The system admin account that has been created
     * @param adminId The ID of the admin user who is creating the account.
     * @param email The email of the admin user who is creating the account
     */
    public void handleNewSystemAdminAccountAction(SystemAdminAccount systemAdminAccount, String adminId,
                                                  ActionResult result, String email) {
        log.info(writeLog(UUID.fromString(adminId),
                          "has attempted to create a System Admin account, which has: " + result.toString()));

        List<String> existingAdminEmails = userRepository.findByRoles(Roles.SYSTEM_ADMIN)
            .stream().map(PiUser::getEmail).toList();

        CreateSystemAdminAction createSystemAdminAction = new CreateSystemAdminAction();
        createSystemAdminAction.setAccountEmail(systemAdminAccount.getEmail());
        createSystemAdminAction.setEmailList(existingAdminEmails);
        createSystemAdminAction.setRequesterEmail(email);
        createSystemAdminAction.setActionResult(result);

        publicationService.sendSystemAdminAccountAction(createSystemAdminAction);
    }

    /**
     * A helper method which specifically handles validation failures on the system admin account.
     * @param account The system admin account to validate.
     * @param issuerId The ID of the admin user that is issuing the account.
     * @param email The email of the admin user requesting the account.
     */
    private void validateSystemAdminAccount(SystemAdminAccount account, String issuerId, String email) {
        Set<ConstraintViolation<SystemAdminAccount>> constraintViolationSet = validator.validate(account);

        if (!constraintViolationSet.isEmpty()) {
            ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setErrorMessages(constraintViolationSet
                                                           .stream().map(constraint -> constraint.getPropertyPath()
                    + ": " + constraint.getMessage()).toList());

            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.FAILED, email);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }

        if (userRepository.findByEmailAndUserProvenance(account.getEmail(), UserProvenances.PI_AAD).isPresent()) {
            ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setDuplicate(true);
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.FAILED, email);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }

        List<PiUser> systemAdminUsers = userRepository.findByRoles(Roles.SYSTEM_ADMIN)
            .stream()
            .filter(u -> UserProvenances.PI_AAD.equals(u.getUserProvenance()))
            .toList();
        if (systemAdminUsers.size() >= maxSystemAdminValue) {
            ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setAboveMaxSystemAdmin(true);
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.ATTEMPTED, email);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }
    }
}
