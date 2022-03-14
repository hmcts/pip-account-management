package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.ForbiddenPermissionsException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSubscriber;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Service layer that deals with the creation of accounts.
 * The storage mechanism (e.g Azure) is seperated into a separate class.
 */
@Slf4j
@Component
@SuppressWarnings("PMD.LawOfDemeter")
public class AccountService {

    private static final String FORBIDDEN_MESSAGE =
        "User: %s does not have sufficient permission to view list type: %s";

    @Autowired
    Validator validator;

    @Autowired
    AzureUserService azureUserService;

    @Autowired
    UserRepository userRepository;

    /**
     * Method to create new subscribers.
     * @return Returns a map which contains two lists, Errored and Created Subscribers. Created will have object ID set.
     **/
    public Map<CreationEnum, List<? extends Subscriber>> createSubscribers(List<Subscriber> subscribers) {

        Map<CreationEnum, List<? extends Subscriber>> processedAccounts = new ConcurrentHashMap<>();

        List<Subscriber> createdAccounts = new ArrayList<>();
        List<ErroredSubscriber> erroredAccounts = new ArrayList<>();

        for (Subscriber subscriber : subscribers) {

            Set<ConstraintViolation<Subscriber>> constraintViolationSet = validator.validate(subscriber);
            if (!constraintViolationSet.isEmpty()) {

                ErroredSubscriber erroredSubscriber = new ErroredSubscriber(subscriber);
                erroredSubscriber.setErrorMessages(constraintViolationSet
                                                       .stream().map(ConstraintViolation::getMessage)
                                                       .collect(Collectors.toList()));
                erroredAccounts.add(erroredSubscriber);
                continue;
            }

            try {
                User user = azureUserService.createUser(subscriber);
                subscriber.setAzureSubscriberId(user.id);

                createdAccounts.add(subscriber);
            } catch (AzureCustomException azureCustomException) {
                ErroredSubscriber erroredSubscriber = new ErroredSubscriber(subscriber);
                erroredSubscriber.setErrorMessages(List.of(azureCustomException.getMessage()));
                erroredAccounts.add(erroredSubscriber);
            }

        }

        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);

        return processedAccounts;
    }

    /**
     * Method to add users to P&I database, loops through the list and validates the email provided then adds them to
     * success or failure lists.
     * @param users the list of users to be added.
     * @param issuerEmail the email of the admin adding the users for logging purposes.
     * @return Map of Created and Errored accounts, created has UUID's and errored has user objects.
     */
    public Map<CreationEnum, List<?>> addUsers(List<PiUser> users, String issuerEmail) {
        List<UUID> createdAccounts = new ArrayList<>();
        List<ErroredPiUser> erroredAccounts = new ArrayList<>();

        for (PiUser user : users) {
            Set<ConstraintViolation<PiUser>> constraintViolationSet = validator.validate(user);
            if (!constraintViolationSet.isEmpty()) {
                ErroredPiUser erroredUser = new ErroredPiUser(user);
                erroredUser.setErrorMessages(constraintViolationSet
                                                 .stream().map(ConstraintViolation::getMessage)
                                                 .collect(Collectors.toList()));
                erroredAccounts.add(erroredUser);
                continue;
            }

            PiUser addedUser = userRepository.save(user);
            createdAccounts.add(addedUser.getUserId());

            log.info(writeLog(issuerEmail, UserActions.CREATE_ACCOUNT, addedUser.getEmail()));
        }

        Map<CreationEnum, List<?>> processedAccounts = new ConcurrentHashMap<>();
        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);
        return processedAccounts;
    }

    /**
     * Used to check if a user can see a given publication based on the provenances of the user and pub.
     * @param userId  the user id of the user to check permissions for.
     * @param listType the list type of the publication used to check which provenances can see classififed.
     * @return bool of true if user can see it, else exception is thrown
     */
    public boolean isUserAuthorisedForPublication(UUID userId, ListType listType) {
        PiUser userToCheck = checkUserReturned(userRepository.findByUserId(userId), userId);
        if (checkAuthorisation(userToCheck.getUserProvenance(), listType)) {
            return true;
        }
        throw new ForbiddenPermissionsException(String.format(FORBIDDEN_MESSAGE, userId, listType));

    }

    /**
     * Checks that the user was found.
     * @param user an optional of the user returned from the database.
     * @param userID the user Id of the user being searched for, for error message purposes.
     * @return the user object if present.
     */
    private PiUser checkUserReturned(Optional<PiUser> user, UUID userID) {
        if (user.isEmpty()) {
            throw new UserNotFoundException("userId", userID.toString());
        }
        return user.get();
    }

    /**
     * Checks the allowed provenance of the list to see if it matches the provenance of the user.
     * @param userProvenance provenance of the user to check.
     * @param listType list type to check against, ENUM value contains the allowed provenance.
     * @return true if list type is available public or private, or if provenances match, else returns false.
     */
    private boolean checkAuthorisation(UserProvenances userProvenance, ListType listType) {
        if (!isGenericListType(listType)) {
            return listType.allowedProvenance.equals(userProvenance);
        }
        return true;
    }

    /**
     * Checks if the list type has no classified restrictions.
     * @param listType list type to check
     * @return true if the list type doesnt require classified restrictions.
     */
    private boolean isGenericListType(ListType listType) {
        return listType.allowedProvenance == null;
    }

    /**
     * Find a user by their provenance and their provenance user id.
     * @param userProvenance the provenance to match
     * @param provenanceUserId the id to search for
     * @return the found user
     */
    public PiUser findUserByProvenanceId(UserProvenances userProvenance, String provenanceUserId) {
        List<PiUser> returnedUser = userRepository.findExistingByProvenanceId(provenanceUserId, userProvenance.name());
        if (!returnedUser.isEmpty()) {
            return returnedUser.get(0);
        }
        throw new UserNotFoundException("provenanceUserId", provenanceUserId);
    }

}
