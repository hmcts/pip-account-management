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
public class SystemAdminAccountService {


    private final Validator validator;
    private final AzureUserService azureUserService;
    private final UserRepository userRepository;
    private final PublicationService publicationService;
    private final AzureAccountService azureAccountService;
    private final Integer maxSystemAdminValue;

    @Autowired
    public SystemAdminAccountService(Validator validator, AzureUserService azureUserService,
                                     UserRepository userRepository, PublicationService publicationService,
                                     @Value("${admin.max-system-admin}")Integer maxSystemAdminValue,
                                     AzureAccountService azureAccountService) {
        this.validator = validator;
        this.azureUserService = azureUserService;
        this.userRepository = userRepository;
        this.publicationService = publicationService;
        this.maxSystemAdminValue = maxSystemAdminValue;
        this.azureAccountService = azureAccountService;
    }

    /**
     * This method deals with the creation of a system admin account.
     * @param account The system admin account to be created.
     * @param issuerId The ID of the user creating the account.
     * @return  The PiUser of the created system admin account.
     */
    public PiUser addSystemAdminAccount(SystemAdminAccount account, String issuerId) {

        String displayName = "";
        String provenanceUserId = verifyAdminUser(issuerId);
        if (!provenanceUserId.isEmpty()) {
            displayName = azureAccountService.retrieveAzureAccount(provenanceUserId).getDisplayName();
        }

        validateSystemAdminAccount(account, issuerId, displayName);
        try {
            User user = azureUserService.createUser(account.convertToAzureAccount(), false);
            PiUser createdUser = userRepository.save(account.convertToPiUser(user.id));
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.SUCCEEDED, displayName);

            publicationService.sendNotificationEmail(
                account.getEmail(),
                account.getFirstName(),
                account.getSurname()
            );
            return createdUser;
        } catch (AzureCustomException e) {
            var erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setErrorMessages(List.of(e.getLocalizedMessage()));
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.FAILED, displayName);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }
    }

    /**
     * This method handles the logging and publishing that a new system admin account has been created.
     * @param systemAdminAccount The system admin account that has been created
     * @param adminId The ID of the admin user who is creating the account.
     * @param name The name of the admin user who is creating the account
     */
    public void handleNewSystemAdminAccountAction(SystemAdminAccount systemAdminAccount, String adminId,
                                                  ActionResult result, String name) {
        log.info(writeLog(UUID.fromString(adminId),
                          "has attempted to create a System Admin account, which has: " + result.toString()));

        List<String> existingAdminEmails = userRepository.findByRoles(Roles.SYSTEM_ADMIN)
            .stream().map(PiUser::getEmail).toList();

        var createSystemAdminAction = new CreateSystemAdminAction();
        createSystemAdminAction.setAccountEmail(systemAdminAccount.getEmail());
        createSystemAdminAction.setEmailList(existingAdminEmails);
        createSystemAdminAction.setRequesterName(name);
        createSystemAdminAction.setActionResult(result);

        publicationService.sendSystemAdminAccountAction(createSystemAdminAction);
    }

    /**
     * A helper method which specifically handles validation failures on the system admin account.
     * @param account The system admin account to validate.
     * @param issuerId The ID of the admin user that is issuing the account.
     * @param name The name of the admin user requesting the account.
     */
    private void validateSystemAdminAccount(SystemAdminAccount account, String issuerId, String name) {
        Set<ConstraintViolation<SystemAdminAccount>> constraintViolationSet = validator.validate(account);

        if (!constraintViolationSet.isEmpty()) {
            var erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setErrorMessages(constraintViolationSet
                                                           .stream().map(constraint -> constraint.getPropertyPath()
                    + ": " + constraint.getMessage()).toList());

            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.FAILED, name);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }

        if (userRepository.findByEmailAndUserProvenance(account.getEmail(), UserProvenances.PI_AAD).isPresent()) {
            var erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setDuplicate(true);
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.FAILED, name);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }

        List<PiUser> systemAdminUsers = userRepository.findByRoles(Roles.SYSTEM_ADMIN);
        if (systemAdminUsers.size() >= maxSystemAdminValue) {
            var erroredSystemAdminAccount = new ErroredSystemAdminAccount(account);
            erroredSystemAdminAccount.setAboveMaxSystemAdmin(true);
            handleNewSystemAdminAccountAction(account, issuerId, ActionResult.ATTEMPTED, name);
            throw new SystemAdminAccountException(erroredSystemAdminAccount);
        }
    }

    /**
     * Method to find whether user is SYSTEM_ADMIN or not.
     * @param issuerId The ID of the admin user
     * @return Boolean user is SYSTEM_ADMIN or not
     */
    private String verifyAdminUser(String issuerId) {
        Optional<PiUser> adminUser = userRepository.findByUserId(UUID.fromString(issuerId));
        if (adminUser.isPresent() && adminUser.get().getRoles().equals(Roles.SYSTEM_ADMIN)) {
            return adminUser.get().getProvenanceUserId();
        }

        return "";
    }
}
