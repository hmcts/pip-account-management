package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.VERIFIED;

@Service
@Slf4j
@SuppressWarnings("PMD.LawOfDemeter")
public class AzureAccountService {
    private static final String EMAIL_NOT_SENT_MESSAGE =
        "Account has been successfully created, however email has failed to send.";

    @Autowired
    Validator validator;

    @Autowired
    AzureUserService azureUserService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PublicationService publicationService;

    /**
     * Method to create new accounts in azure.
     *
     * @param azureAccounts         The accounts to be created.
     * @param issuerId              The id of the user who created the accounts.
     * @param useSuppliedPassword   Create password using the supplied value
     * @return Returns a map which contains two lists, Errored and Created accounts. Created will have object ID set.
     **/
    public Map<CreationEnum, List<? extends AzureAccount>> addAzureAccounts(//NOSONAR
        List<AzureAccount> azureAccounts, String issuerId, boolean isExisting, boolean useSuppliedPassword) {

        Map<CreationEnum, List<? extends AzureAccount>> processedAccounts = new ConcurrentHashMap<>();

        List<AzureAccount> createdAzureAccounts = new ArrayList<>();
        List<ErroredAzureAccount> erroredAccounts = new ArrayList<>();

        for (AzureAccount azureAccount : azureAccounts) {
            Set<ConstraintViolation<AzureAccount>> constraintViolationSet = validator.validate(azureAccount);
            if (!constraintViolationSet.isEmpty()) {
                checkAndAddToErrorAccount(false, azureAccount, constraintViolationSet
                                              .stream().map(constraint -> constraint.getPropertyPath()
                                                  + ": " + constraint.getMessage()).toList(),
                                          erroredAccounts);
                continue;
            }

            try {
                if (!checkUserAlreadyExists(azureAccount, erroredAccounts)) {
                    User user = azureUserService.createUser(azureAccount, useSuppliedPassword);

                    azureAccount.setAzureAccountId(user.id);
                    createdAzureAccounts.add(azureAccount);

                    log.info(writeLog(issuerId, UserActions.CREATE_ACCOUNT, azureAccount.getAzureAccountId()));
                    boolean emailSent = handleAccountCreationEmail(azureAccount, user.givenName, isExisting);
                    checkAndAddToErrorAccount(emailSent, azureAccount, List.of(EMAIL_NOT_SENT_MESSAGE),
                                              erroredAccounts);
                }
            } catch (AzureCustomException azureCustomException) {
                log.error(writeLog(issuerId, UserActions.CREATE_ACCOUNT, azureAccount.getAzureAccountId()));
                checkAndAddToErrorAccount(false, azureAccount, List.of(azureCustomException.getMessage()),
                                          erroredAccounts);
            }
        }

        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAzureAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);

        return processedAccounts;
    }

    public AzureAccount retrieveAzureAccount(String provenanceUserId) {
        try {
            Optional<PiUser> user = userRepository.findByProvenanceUserIdAndUserProvenance(
                provenanceUserId, UserProvenances.PI_AAD);
            if (user.isPresent()) {
                AzureAccount azureAccount = new AzureAccount();
                User aadUser = azureUserService.getUser(user.get().getEmail());
                azureAccount.setAzureAccountId(aadUser.id);
                azureAccount.setFirstName(aadUser.givenName);
                azureAccount.setSurname(aadUser.surname);
                azureAccount.setDisplayName(aadUser.displayName);
                azureAccount.setEmail(user.get().getEmail());
                return azureAccount;
            } else {
                throw new NotFoundException(String.format(
                    "User with supplied provenanceUserId: %s could not be found", provenanceUserId));
            }
        } catch (AzureCustomException e) {
            log.error(writeLog(UUID.fromString(provenanceUserId), "Error while retrieving users details"));
        }

        throw new IllegalArgumentException("Error while retrieving user details with provenanceUserId: "
                                               + provenanceUserId);
    }

    private boolean checkUserAlreadyExists(AzureAccount azureAccount, List<ErroredAzureAccount> erroredAccounts)
        throws AzureCustomException {
        User userAzure = azureUserService.getUser(azureAccount.getEmail());

        if (userAzure != null && !userAzure.givenName.isEmpty()
            && azureAccount.getRole().equals(VERIFIED)) {
            boolean emailSent = publicationService.sendNotificationEmailForDuplicateMediaAccount(
                azureAccount.getEmail(), userAzure.givenName);

            checkAndAddToErrorAccount(emailSent, azureAccount,
                                      List.of("Unable to send duplicate media account email"),
                                      erroredAccounts);
            return true;
        }
        return false;
    }

    private void checkAndAddToErrorAccount(boolean checkCondition, AzureAccount azureAccount, List<String> errorMessage,
                                           List<ErroredAzureAccount> erroredAccounts) {
        if (!checkCondition) {
            ErroredAzureAccount softErroredAccount = new ErroredAzureAccount(azureAccount);
            softErroredAccount.setErrorMessages(errorMessage);
            erroredAccounts.add(softErroredAccount);
        }
    }

    private boolean handleAccountCreationEmail(AzureAccount createdAccount, String fullName,
                                               boolean isExisting) {
        return switch (createdAccount.getRole()) {
            case INTERNAL_ADMIN_CTSC, INTERNAL_ADMIN_LOCAL, INTERNAL_SUPER_ADMIN_CTSC, INTERNAL_SUPER_ADMIN_LOCAL ->
                publicationService.sendNotificationEmail(createdAccount.getEmail(),
                                                         createdAccount.getFirstName(), createdAccount.getSurname());
            case VERIFIED ->  publicationService.sendMediaNotificationEmail(createdAccount.getEmail(),
                                                                            fullName, isExisting);
            default -> false;
        };
    }
}
