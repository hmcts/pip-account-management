package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.helpers.EmailHelper;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.MediaCsv;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;
import uk.gov.hmcts.reform.pip.account.management.service.helpers.DateTimeHelper;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;

import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_NON_RESTRICTED_ADMIN_ROLES;
import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_NON_THIRD_PARTY_ROLES;
import static uk.gov.hmcts.reform.pip.account.management.model.UserProvenances.ALL_NON_THIRD_PARTY_PROVENANCES;
import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Service layer that deals with the creation of accounts.
 * The storage mechanism (e.g Azure) is seperated into a separate class.
 */
@Slf4j
@Component
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.ExcessiveImports", "PMD.TooManyMethods"})
public class AccountService {

    @Autowired
    Validator validator;

    @Autowired
    AzureUserService azureUserService;

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

    private static final String EMAIL_NOT_SENT_MESSAGE =
        "Account has been successfully created, however email has failed to send.";

    /**
     * Method to create new accounts in azure.
     *
     * @param azureAccounts The accounts to be created.
     * @param issuerId      The id of the user who created the accounts.
     * @return Returns a map which contains two lists, Errored and Created accounts. Created will have object ID set.
     **/
    public Map<CreationEnum, List<? extends AzureAccount>> addAzureAccounts(
        List<AzureAccount> azureAccounts, String issuerId, boolean isExisting) {

        Map<CreationEnum, List<? extends AzureAccount>> processedAccounts = new ConcurrentHashMap<>();

        List<AzureAccount> createdAzureAccounts = new ArrayList<>();
        List<ErroredAzureAccount> erroredAccounts = new ArrayList<>();

        for (AzureAccount azureAccount : azureAccounts) {

            Set<ConstraintViolation<AzureAccount>> constraintViolationSet = validator.validate(azureAccount);
            if (!constraintViolationSet.isEmpty()) {

                ErroredAzureAccount erroredSubscriber = new ErroredAzureAccount(azureAccount);
                erroredSubscriber.setErrorMessages(constraintViolationSet
                                                       .stream().map(constraint -> constraint.getPropertyPath()
                        + ": " + constraint.getMessage()).collect(Collectors.toList()));
                erroredAccounts.add(erroredSubscriber);
                continue;
            }

            try {

                User userAzure = azureUserService.getUser(azureAccount.getEmail());

                if (userAzure != null && !userAzure.givenName.isEmpty()
                    && azureAccount.getRole().equals(Roles.VERIFIED)) {

                    boolean emailSent = publicationService.sendNotificationEmailForDuplicateMediaAccount(
                            azureAccount.getEmail(), userAzure.givenName);

                    if (!emailSent) {
                        ErroredAzureAccount softErroredAccount = new ErroredAzureAccount(azureAccount);
                        softErroredAccount.setErrorMessages(
                            List.of("Unable to send duplicate media account email"));
                        erroredAccounts.add(softErroredAccount);
                    }
                    continue;
                }

                User user = azureUserService.createUser(azureAccount);
                azureAccount.setAzureAccountId(user.id);
                createdAzureAccounts.add(azureAccount);

                log.info(writeLog(issuerId, UserActions.CREATE_ACCOUNT, azureAccount.getAzureAccountId()));

                if (!handleAccountCreationEmail(azureAccount, user.givenName, isExisting)) {
                    ErroredAzureAccount softErroredAccount = new ErroredAzureAccount(azureAccount);
                    softErroredAccount.setErrorMessages(List.of(EMAIL_NOT_SENT_MESSAGE));
                    erroredAccounts.add(softErroredAccount);
                }

            } catch (AzureCustomException azureCustomException) {
                log.error(writeLog(issuerId, UserActions.CREATE_ACCOUNT, azureAccount.getAzureAccountId()));
                ErroredAzureAccount erroredSubscriber = new ErroredAzureAccount(azureAccount);
                erroredSubscriber.setErrorMessages(List.of(azureCustomException.getMessage()));
                erroredAccounts.add(erroredSubscriber);
            }

        }

        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAzureAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);

        return processedAccounts;
    }

    /**
     * Method to add users to P&I database, loops through the list and validates the email provided then adds them to
     * success or failure lists.
     *
     * @param users    the list of users to be added.
     * @param issuerId the id of the admin adding the users for logging purposes.
     * @return Map of Created and Errored accounts, created has UUID's and errored has user objects.
     */
    public Map<CreationEnum, List<?>> addUsers(List<PiUser> users, String issuerId) {
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
                continue;
            }

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

            log.info(writeLog(issuerId, UserActions.CREATE_ACCOUNT, addedUser.getUserId().toString()));
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
        throw new UserNotFoundException("provenanceUserId", provenanceUserId);
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

    public Map<CreationEnum, List<?>> uploadMediaFromCsv(MultipartFile mediaCsv, String issuerId) {
        List<MediaCsv> mediaList;

        try (InputStreamReader inputStreamReader = new InputStreamReader(mediaCsv.getInputStream());
             Reader reader = new BufferedReader(inputStreamReader)) {

            CsvToBean<MediaCsv> csvToBean = new CsvToBeanBuilder<MediaCsv>(reader)
                .withType(MediaCsv.class)
                .build();

            mediaList = csvToBean.parse();
        } catch (Exception ex) {
            throw new CsvParseException(ex.getMessage());
        }

        return addToAzureAndPiUsers(mediaList, issuerId);
    }

    private Map<CreationEnum, List<?>> addToAzureAndPiUsers(List<MediaCsv> accounts, String issuerId) {
        Map<CreationEnum, List<? extends AzureAccount>> azureAccounts;
        Map<CreationEnum, List<?>> piUserAccounts;
        Map<CreationEnum, List<?>> completedAccounts = new ConcurrentHashMap<>();

        azureAccounts = addAzureAccounts(accountModelMapperService.createAzureUsersFromCsv(accounts),
                                         issuerId, true
        );
        piUserAccounts = addUsers(
            accountModelMapperService
                .createPiUsersFromAzureAccounts(azureAccounts.get(CreationEnum.CREATED_ACCOUNTS)),
            issuerId
        );


        completedAccounts.put(
            CreationEnum.CREATED_ACCOUNTS,
            piUserAccounts.get(CreationEnum.CREATED_ACCOUNTS)
        );
        completedAccounts.put(
            CreationEnum.ERRORED_ACCOUNTS,
            Stream.concat(
                azureAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream(),
                piUserAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream()
            ).distinct().collect(Collectors.toList())
        );
        return completedAccounts;
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

    public String getAccManDataForMiReporting() {
        StringBuilder builder = new StringBuilder(54);
        builder.append("user_id,provenance_user_id,user_provenance,roles").append(System.lineSeparator());
        userRepository.getAccManDataForMI()
            .forEach(line -> builder.append(line).append(System.lineSeparator()));
        return builder.toString();
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
            if (UserProvenances.PI_AAD.equals(userToDelete.getUserProvenance())) {
                azureUserService.deleteUser(userToDelete.getProvenanceUserId());
            }
        } catch (AzureCustomException ex) {
            log.info(writeLog(String.format("Error when deleting an account from azure with Provenance user id: "
                                                + "%s and error: %s",
                                            userToDelete.getProvenanceUserId(), ex.getMessage())));
        }

        log.info(writeLog(
            subscriptionService.sendSubscriptionDeletionRequest(userToDelete.getUserId().toString()))
        );
        userRepository.delete(userToDelete);
        return String.format("User with ID %s has been deleted", userToDelete.getUserId());
    }

    /**
     * Update an user account by the supplied provenance id.
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
     * Request a page of users that can be filtered down with the supplied parameters.
     *
     * @param pageable The pageable object.
     * @param email The email to query by.
     * @param userProvenanceId The user provenance id to query by.
     * @param userProvenances A list of user provenances to query by.
     * @param roles A list of roles to query by.
     * @param userId The user id to query by.
     * @return A page with a list of piUsers.
     */
    public Page<PiUser> findAllAccountsExceptThirdParty(Pageable pageable, String email, String userProvenanceId,
                                                        List<UserProvenances> userProvenances, List<Roles> roles,
                                                        String userId) {

        // If a user ID is supplied then only find by that
        if (!userId.isBlank()) {
            return userRepository.findByUserIdPageable(userId, pageable);
        }

        //  If a list of user provenances are supplied then use that else use all non third party user provenances
        List<UserProvenances> userProvenancesToQuery = ALL_NON_THIRD_PARTY_PROVENANCES;
        if (!userProvenances.isEmpty()) {
            userProvenancesToQuery = userProvenances;
        }

        // If a list of roles are supplied then use that else use all non third party roles
        List<Roles> rolesToQuery = ALL_NON_THIRD_PARTY_ROLES;
        if (!roles.isEmpty()) {
            rolesToQuery = roles;
        }

        // If user provenance id is supplied then find by an exact match
        String userProvenanceIdToQuery = "%%";
        if (!userProvenanceId.isBlank()) {
            userProvenanceIdToQuery = userProvenanceId;
        }

        return userRepository.findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
            "%" + email + "%",
            userProvenancesToQuery,
            rolesToQuery,
            userProvenanceIdToQuery,
            pageable
        );
    }

    /**
     * Process updating a role for an account.
     *
     * @param userId The ID of the user to update.
     * @param updatedRole The updated role of the user.
     * @return A confirmation string that the user has been updated with the new role.
     */
    public String updateAccountRole(UUID userId, Roles updatedRole) {
        PiUser userToUpdate = userRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException(String.format(
            "User with supplied user id: %s could not be found", userId)));

        // If they are a PI AAD user then try update the users role in B2C
        if (UserProvenances.PI_AAD.equals(userToUpdate.getUserProvenance())) {
            try {
                azureUserService.updateUserRole(userToUpdate.getProvenanceUserId(), updatedRole.toString());
            } catch (AzureCustomException ex) {
                log.info(String.format("Failed to update user with ID %s in Azure", userId));
            }
        }

        userToUpdate.setRoles(updatedRole);
        userRepository.save(userToUpdate);

        String returnMessage = String.format("User with ID %s has been updated to a %s", userId, updatedRole);
        log.info(returnMessage);

        return returnMessage;
    }

    /**
     * Method which will retrieve all accounts which are third party.
     *
     * @return The list of found PiUser accounts.
     */
    public List<PiUser> findAllThirdPartyAccounts() {
        return userRepository.findAllByUserProvenance(UserProvenances.THIRD_PARTY);
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
     * This method retrieves an admin user (excluding system admin) by their email and provenance.
     * @param email The email of the user to retrieve
     * @param provenance The provenance of the user to retrieve
     * @return The user that is found.
     * @throws NotFoundException if a user is not found.
     */
    public PiUser getAdminUserByEmailAndProvenance(String email, UserProvenances provenance) {
        return userRepository.findByEmailIgnoreCaseAndUserProvenanceAndRolesIn(email, provenance,
                                                                               ALL_NON_RESTRICTED_ADMIN_ROLES)
            .orElseThrow(() -> new NotFoundException(String.format(
                "No user found with the email: %s and provenance %s", EmailHelper.maskEmail(email), provenance)));
    }
}
