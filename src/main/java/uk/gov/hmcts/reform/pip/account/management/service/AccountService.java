package uk.gov.hmcts.reform.pip.account.management.service;

import io.micrometer.common.util.StringUtils;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.ForbiddenRoleUpdateException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserWithProvenanceNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;
import uk.gov.hmcts.reform.pip.account.management.service.helpers.DateTimeHelper;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;

/**
 * Service layer that deals with the creation of accounts.
 * The storage mechanism (e.g Azure) is seperated into a separate class.
 */
@Slf4j
@Service
@SuppressWarnings("PMD.TooManyMethods")
public class AccountService {

    private static final int MAX_PAGE_SIZE = 25;

    @Autowired
    Validator validator;

    @Autowired
    AzureUserService azureUserService;

    @Autowired
    AzureAccountService azureAccountService;

    @Autowired
    AccountFilteringService accountFilteringService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    PublicationService publicationService;

    @Autowired
    SensitivityService sensitivityService;

    @Autowired
    AccountModelMapperService accountModelMapperService;

    @Autowired
    SubscriptionService subscriptionService;

    /**
     * Method to add users to P&I database, loops through the list and validates the email provided then adds them to
     * success or failure lists.
     *
     * @param users    the list of users to be added.
     * @param issuerId the id of the admin adding the users for logging purposes.
     * @return Map of Created and Errored accounts, created has UUID's and errored has user objects.
     */
    public Map<CreationEnum, List<?>> addUsers(List<PiUser> users, String issuerId) { //NOSONAR
        List<UUID> createdAccounts = new ArrayList<>();
        List<ErroredPiUser> erroredAccounts = new ArrayList<>();

        for (PiUser user : users) {
            LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("UTC"));
            user.setLastVerifiedDate(localDateTime);
            user.setLastSignedInDate(localDateTime);
            user.setCreatedDate(localDateTime);
            Set<ConstraintViolation<PiUser>> constraintViolationSet = validator.validate(user);

            if (user.getRoles().equals(Roles.SYSTEM_ADMIN)) {
                ErroredPiUser erroredUser = new ErroredPiUser(user);
                erroredUser.setErrorMessages(List.of(
                    "System admins must be created via the /account/add/system-admin endpoint"));
                erroredAccounts.add(erroredUser);
            } else if (constraintViolationSet.isEmpty()) {
                PiUser addedUser = userRepository.save(user);
                createdAccounts.add(addedUser.getUserId());
                log.info(writeLog(issuerId, UserActions.CREATE_ACCOUNT, addedUser.getUserId().toString()));
            } else {
                ErroredPiUser erroredUser = new ErroredPiUser(user);
                erroredUser.setErrorMessages(constraintViolationSet
                                                 .stream().map(ConstraintViolation::getMessage)
                                                 .toList());
                erroredAccounts.add(erroredUser);
            }
        }

        Map<CreationEnum, List<?>> processedAccounts = new ConcurrentHashMap<>();
        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);
        return processedAccounts;
    }

    /**
     * Used to check if a user can see a given publication based on the provenances of the user,
     * and list type / sensitivity of the publication.
     *
     * @param userId      the user id of the user to check permissions for.
     * @param listType    the list type of the publication.
     * @param sensitivity the sensitivity of the publication.
     * @return bool of true if user can see it, else exception is thrown
     */
    public boolean isUserAuthorisedForPublication(UUID userId, ListType listType, Sensitivity sensitivity) {
        PiUser userToCheck = checkUserReturned(userRepository.findByUserId(userId), userId);
        return sensitivityService.checkAuthorisation(userToCheck, listType, sensitivity);

    }

    /**
     * Checks that the user was found.
     *
     * @param user   an optional of the user returned from the database.
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
     * Find a user by their provenance and their provenance user id.
     *
     * @param userProvenance   the provenance to match
     * @param provenanceUserId the id to search for
     * @return the found user
     */
    public PiUser findUserByProvenanceId(UserProvenances userProvenance, String provenanceUserId) {
        List<PiUser> returnedUser = userRepository.findExistingByProvenanceId(provenanceUserId, userProvenance.name());
        if (!returnedUser.isEmpty()) {
            return returnedUser.get(0);
        }
        throw new UserWithProvenanceNotFoundException(provenanceUserId);
    }

    public Map<String, Optional<String>> findUserEmailsByIds(List<String> userIdsList) {
        Map<String, Optional<String>> emailMap = new ConcurrentHashMap<>();
        for (String userId : userIdsList) {
            Optional<PiUser> returnedUser = userRepository.findByUserId(UUID.fromString(userId));

            returnedUser.ifPresent(value ->
                                       emailMap.put(userId, Optional.ofNullable(value.getEmail()))
            );

            emailMap.computeIfAbsent(userId, v -> Optional.empty());
        }
        return emailMap;
    }

    /**
     * Delete a user account by the supplied email.
     * This deletes the user from AAD, our user table and subscriptions.
     *
     * @param userId The ID of the user to delete.
     * @return Confirmation message that account has been deleted.
     */
    public String deleteAccount(UUID userId) {
        PiUser userToDelete = userRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException("User with supplied ID could not be found"));

        try {
            if (PI_AAD.equals(userToDelete.getUserProvenance())) {
                azureUserService.deleteUser(userToDelete.getProvenanceUserId());
            }
            log.info(writeLog(UserActions.REMOVE_ACCOUNT, userId.toString()));
        } catch (AzureCustomException ex) {
            log.error(writeLog(String.format("Error when deleting an account from azure with Provenance user id: "
                                                + "%s and error: %s",
                                            userToDelete.getProvenanceUserId(), ex.getMessage())));
        }

        subscriptionService.sendSubscriptionDeletionRequest(userToDelete.getUserId().toString());
        userRepository.delete(userToDelete);
        return String.format("User with ID %s has been deleted", userToDelete.getUserId());
    }

    public String deleteAllAccountsWithEmailPrefix(String prefix) {
        List<UUID> allUserIds = new ArrayList<>();
        boolean noMoreAccounts = false;

        int pageCount = 0;
        do {
            List<UUID> userIds = accountFilteringService.findAllAccountsExceptThirdParty(
                PageRequest.of(pageCount, MAX_PAGE_SIZE), prefix, "",
                Collections.emptyList(), Collections.emptyList(), ""
            ).stream().map(PiUser::getUserId).toList();

            allUserIds.addAll(userIds);
            pageCount++;

            if (userIds.size() < MAX_PAGE_SIZE) {
                noMoreAccounts = true;
            }
        } while (!noMoreAccounts);

        allUserIds.forEach(i -> deleteAccount(i));
        return String.format("%s account(s) deleted with email starting with %s", allUserIds.size(), prefix);
    }

    /**
     * Update a user account by the supplied provenance id.
     *
     * @param userProvenance   The user provenance of the user to update.
     * @param provenanceUserId The provenance id of the user to update.
     * @return Confirmation message that the user account has been updated.
     */
    public String updateAccount(UserProvenances userProvenance, String provenanceUserId, Map<String, String> params) {
        PiUser userToUpdate = userRepository.findByProvenanceUserIdAndUserProvenance(provenanceUserId, userProvenance)
            .orElseThrow(() -> new NotFoundException(String.format(
                "User with supplied provenance id: %s could not be found", provenanceUserId)));

        params.forEach((k, v) -> {
            try {
                switch (k) {
                    case "lastVerifiedDate" -> userToUpdate
                        .setLastVerifiedDate(DateTimeHelper.zonedDateTimeStringToLocalDateTime(v));
                    case "lastSignedInDate" -> userToUpdate
                        .setLastSignedInDate(DateTimeHelper.zonedDateTimeStringToLocalDateTime(v));
                    default -> throw new IllegalArgumentException(String.format(
                        "The field '%s' could not be updated", k));
                }
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException(String.format("Date time value '%s' not in expected format", v));
            }
        });

        userRepository.save(userToUpdate);
        return String.format("Account with provenance %s and provenance id %s has been updated",
                             userProvenance.name(), provenanceUserId
        );
    }

    /**
     * Process updating a role for an account.
     *
     * @param adminId The Admin ID of the user who is performing the action.
     * @param userId The ID of the user to update.
     * @param updatedRole The updated role of the user.
     * @return A confirmation string that the user has been updated with the new role.
     */
    public String updateAccountRole(UUID adminId, UUID userId, Roles updatedRole) {
        if (adminId != null && adminId.equals(userId)) {
            throw new ForbiddenRoleUpdateException(
                String.format("User with id %s is unable to update user ID %s", adminId, userId));
        }

        PiUser userToUpdate = userRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException(String.format(
            "User with supplied user id: %s could not be found", userId)));

        // If they are a PI AAD user then try update the users role in B2C
        if (PI_AAD.equals(userToUpdate.getUserProvenance())) {
            try {
                azureUserService.updateUserRole(userToUpdate.getProvenanceUserId(), updatedRole.toString());
            } catch (AzureCustomException ex) {
                log.error(writeLog(
                    String.format("Failed to update user with ID %s in Azure", userToUpdate.getUserId())
                ));
            }
        }

        userToUpdate.setRoles(updatedRole);
        userRepository.save(userToUpdate);

        String returnMessage = String.format(
            "User with ID %s has been updated to a %s", userToUpdate.getUserId(), updatedRole);
        log.info(writeLog(returnMessage));

        return returnMessage;
    }

    /**
     * This method retrieves a user by their ID.
     * @param userId The user ID to retrieve
     * @return The user that is found.
     * @throws NotFoundException if a user is not found.
     */
    public PiUser getUserById(UUID userId) {
        return userRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException(String.format(
            "User with supplied user id: %s could not be found", userId)));
    }

    /**
     * Create a new user with the supplied information (including the password). This is only used for testing support.
     *
     * @param azureAccount  The user to be added.
     * @return The created account wrapped in Optional if success, otherwise nothing.
     */
    public Pair<CreationEnum, Object> addUserWithSuppliedPassword(AzureAccount azureAccount, String issuerId) {
        if (StringUtils.isBlank(azureAccount.getPassword())) {
            return Pair.of(CreationEnum.ERRORED_ACCOUNTS, "Password must not be blank");
        }

        Map<CreationEnum, List<? extends AzureAccount>> processedAzureAccounts = azureAccountService
            .addAzureAccounts(List.of(azureAccount), issuerId, false, true);

        if (!processedAzureAccounts.get(CreationEnum.CREATED_ACCOUNTS).isEmpty()) {
            List<AzureAccount> createdAzureAccounts = processedAzureAccounts.get(CreationEnum.CREATED_ACCOUNTS).stream()
                .map(a -> (AzureAccount) a)
                .toList();

            PiUser user = createdAccountToPiUser(createdAzureAccounts.get(0));
            Map<CreationEnum, List<?>> processedPiAccounts = addUsers(List.of(user), issuerId);

            if (processedPiAccounts.get(CreationEnum.CREATED_ACCOUNTS).isEmpty()) {
                return Pair.of(
                    CreationEnum.ERRORED_ACCOUNTS,
                    processedPiAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream()
                        .map(a -> (ErroredPiUser) a)
                        .toList().get(0)
                        .getErrorMessages().toString()
                );
            } else {
                List<UUID> createdPiUserIds = processedPiAccounts.get(CreationEnum.CREATED_ACCOUNTS).stream()
                    .map(a -> (UUID) a)
                    .toList();
                user.setUserId(createdPiUserIds.get(0));
                return Pair.of(CreationEnum.CREATED_ACCOUNTS, user);
            }
        }
        return Pair.of(
            CreationEnum.ERRORED_ACCOUNTS,
            processedAzureAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream()
                .map(a -> (ErroredAzureAccount) a)
                .toList().get(0)
                .getErrorMessages().toString()
        );
    }

    private PiUser createdAccountToPiUser(AzureAccount createdAccount) {
        PiUser user = new PiUser();
        user.setEmail(createdAccount.getEmail());
        user.setForenames(createdAccount.getFirstName());
        user.setSurname(createdAccount.getSurname());
        user.setRoles(createdAccount.getRole());
        user.setProvenanceUserId(createdAccount.getAzureAccountId());
        user.setUserProvenance(PI_AAD);
        return user;
    }
}
