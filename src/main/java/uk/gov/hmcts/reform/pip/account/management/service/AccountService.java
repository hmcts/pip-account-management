package uk.gov.hmcts.reform.pip.account.management.service;

import com.microsoft.graph.models.User;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.reform.pip.account.management.database.MediaApplicationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.AzureCustomException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaCsv;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredPiUser;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
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

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Service layer that deals with the creation of accounts.
 * The storage mechanism (e.g Azure) is seperated into a separate class.
 */
@Slf4j
@Component
@SuppressWarnings("PMD.LawOfDemeter")
public class AccountService {

    @Autowired
    Validator validator;

    @Autowired
    AzureUserService azureUserService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    MediaApplicationRepository mediaApplicationRepository;

    @Autowired
    PublicationService publicationService;

    @Autowired
    SensitivityService sensitivityService;

    @Autowired
    AccountModelMapperService accountModelMapperService;

    private static final String EMAIL_NOT_SENT_MESSAGE =
        "Account has been successfully created, however email has failed to send.";

    /**
     * Method to create new accounts in azure.
     *
     * @param azureAccounts The accounts to be created.
     * @param issuerEmail The email of the user who created the accounts.
     *
     * @return Returns a map which contains two lists, Errored and Created accounts. Created will have object ID set.
     **/
    public Map<CreationEnum, List<? extends AzureAccount>> addAzureAccounts(
        List<AzureAccount> azureAccounts, String issuerEmail, boolean isExisting) {

        Map<CreationEnum, List<? extends AzureAccount>> processedAccounts = new ConcurrentHashMap<>();

        List<AzureAccount> createdAzureAccounts = new ArrayList<>();
        List<ErroredAzureAccount> erroredAccounts = new ArrayList<>();

        for (AzureAccount azureAccount : azureAccounts) {

            Set<ConstraintViolation<AzureAccount>> constraintViolationSet = validator.validate(azureAccount);
            if (!constraintViolationSet.isEmpty()) {

                ErroredAzureAccount erroredSubscriber = new ErroredAzureAccount(azureAccount);
                erroredSubscriber.setErrorMessages(constraintViolationSet
                             .stream().map(constraint -> constraint.getPropertyPath() + ": " + constraint.getMessage())
                                                       .collect(Collectors.toList()));
                erroredAccounts.add(erroredSubscriber);
                continue;
            }

            try {
                User user = azureUserService.createUser(azureAccount);
                azureAccount.setAzureAccountId(user.id);
                createdAzureAccounts.add(azureAccount);

                log.info(writeLog(issuerEmail, UserActions.CREATE_ACCOUNT, azureAccount.getEmail()));

                if (!handleAccountCreationEmail(azureAccount, isExisting)) {
                    ErroredAzureAccount softErroredAccount = new ErroredAzureAccount(azureAccount);
                    softErroredAccount.setErrorMessages(List.of(EMAIL_NOT_SENT_MESSAGE));
                    erroredAccounts.add(softErroredAccount);
                }

            } catch (AzureCustomException azureCustomException) {
                log.error(writeLog(issuerEmail, UserActions.CREATE_ACCOUNT, azureAccount.getEmail()));
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

            Optional<MediaApplication> application =
                mediaApplicationRepository.findByEmail(user.getEmail());

            if (userRepository.findByEmail(user.getEmail()).isPresent()
                && application.isPresent()) {
                boolean emailSent = publicationService.sendNotificationEmailForDuplicateMediaAccount(user.getEmail(),
                                                                            application.get().getFullName());
                if (!emailSent) {
                    ErroredPiUser erroredUser = new ErroredPiUser(user);
                    erroredUser.setErrorMessages(Arrays.asList("Unable to send duplicate media account email"));
                    erroredAccounts.add(erroredUser);
                }

                continue;
            }

            PiUser addedUser = userRepository.save(user);
            createdAccounts.add(addedUser.getUserId());

            if (application.isPresent()) {
                publicationService.sendNotificationEmailForSetupMediaAccount(user.getEmail(),
                                                                             application.get().getFullName());
            }

            log.info(writeLog(issuerEmail, UserActions.CREATE_ACCOUNT, addedUser.getEmail()));
        }

        Map<CreationEnum, List<?>> processedAccounts = new ConcurrentHashMap<>();
        processedAccounts.put(CreationEnum.CREATED_ACCOUNTS, createdAccounts);
        processedAccounts.put(CreationEnum.ERRORED_ACCOUNTS, erroredAccounts);
        return processedAccounts;
    }

    /**
     * Used to check if a user can see a given publication based on the provenances of the user,
     * and list type / sensitivity of the publication.
     * @param userId  the user id of the user to check permissions for.
     * @param listType the list type of the publication.
     * @param sensitivity the sensitivity of the publication.
     * @return bool of true if user can see it, else exception is thrown
     */
    public boolean isUserAuthorisedForPublication(UUID userId, ListType listType, Sensitivity sensitivity) {
        PiUser userToCheck = checkUserReturned(userRepository.findByUserId(userId), userId);
        return sensitivityService.checkAuthorisation(userToCheck, listType, sensitivity);

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

    public Map<CreationEnum, List<?>> uploadMediaFromCsv(MultipartFile mediaCsv, String issuerEmail) {
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

        return addToAzureAndPiUsers(mediaList, issuerEmail);
    }

    private Map<CreationEnum, List<?>> addToAzureAndPiUsers(List<MediaCsv> accounts, String issuerEmail) {
        Map<CreationEnum, List<? extends AzureAccount>> azureAccounts;
        Map<CreationEnum, List<?>> piUserAccounts;
        Map<CreationEnum, List<?>> completedAccounts = new ConcurrentHashMap<>();

        azureAccounts = addAzureAccounts(accountModelMapperService.createAzureUsersFromCsv(accounts),
                                         issuerEmail, true);
        piUserAccounts = addUsers(
            accountModelMapperService
                .createPiUsersFromAzureAccounts(azureAccounts.get(CreationEnum.CREATED_ACCOUNTS)),
            issuerEmail);


        completedAccounts.put(
            CreationEnum.CREATED_ACCOUNTS,
            piUserAccounts.get(CreationEnum.CREATED_ACCOUNTS)
        );
        completedAccounts.put(
            CreationEnum.ERRORED_ACCOUNTS,
            Stream.concat(
                azureAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream(),
                piUserAccounts.get(CreationEnum.ERRORED_ACCOUNTS).stream()
            ).distinct().collect(Collectors.toList()));
        return completedAccounts;
    }

    private boolean handleAccountCreationEmail(AzureAccount createdAccount, boolean isExisting) {
        boolean isSuccessful;
        switch (createdAccount.getRole()) {
            case INTERNAL_ADMIN_CTSC:
            case INTERNAL_ADMIN_LOCAL:
            case INTERNAL_SUPER_ADMIN_CTSC:
            case INTERNAL_SUPER_ADMIN_LOCAL:
                isSuccessful = publicationService.sendNotificationEmail(createdAccount.getEmail(),
                                                                  createdAccount.getFirstName(),
                                                                  createdAccount.getSurname());
                break;
            case VERIFIED:
                isSuccessful = publicationService.sendMediaNotificationEmail(createdAccount.getEmail(), isExisting);
                break;

            default:
                isSuccessful = false;
                break;
        }
        return isSuccessful;
    }

}
