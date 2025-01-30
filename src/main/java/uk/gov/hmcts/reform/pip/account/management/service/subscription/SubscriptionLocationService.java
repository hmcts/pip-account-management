package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;


@Slf4j
@Service
public class SubscriptionLocationService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionListTypeRepository subscriptionListTypeRepository;
    private final PublicationService publicationService;
    private final AccountService accountService;
    private final UserRepository userRepository;


    @Autowired
    public SubscriptionLocationService(
        PublicationService publicationService,
        AccountService accountService,
        UserRepository userRepository,
        SubscriptionRepository subscriptionRepository,
        SubscriptionListTypeRepository subscriptionListTypeRepository
    ) {
        this.publicationService = publicationService;
        this.accountService = accountService;
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
    }

    public List<Subscription> findSubscriptionsByLocationId(String value) {
        List<Subscription> locationSubscriptions = subscriptionRepository.findSubscriptionsByLocationId(value);
        if (locationSubscriptions.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the location id %s",
                value
            ));
        }
        return locationSubscriptions;
    }

    public String deleteSubscriptionByLocation(String locationId, String userId) {
        log.info(writeLog(String.format("User %s attempting to delete all subscriptions for location %s",
                                        userId, locationId)));
        List<Subscription> locationSubscriptions = findSubscriptionsByLocationId(locationId);

        List<UUID> subIds = locationSubscriptions.stream()
            .map(Subscription::getId).toList();
        subscriptionRepository.deleteByIdIn(subIds);

        //DELETE DATA FROM SUBSCRIPTION LIST TYPE TABLE AS WELL.
        this.deleteAllSubscriptionListTypeForLocation(locationSubscriptions);

        log.info(writeLog(String.format("%s subscription(s) have been deleted for location %s by user %s",
                                        subIds.size(), locationId, userId)));

        notifySubscriberAboutSubscriptionDeletion(locationSubscriptions, locationId);
        notifySystemAdminAboutSubscriptionDeletion(userId,
            String.format("Total %s subscription(s) for location ID %s",
                          locationSubscriptions.size(), locationId));

        return String.format("Total %s subscriptions deleted for location id %s", subIds.size(), locationId);
    }

    private void deleteAllSubscriptionListTypeForLocation(List<Subscription> locationSubscriptions) {
        List<String> uniqueUsers = locationSubscriptions.stream()
            .map(Subscription::getUserId).distinct().toList();

        if (!uniqueUsers.isEmpty()) {

            for (String userId : uniqueUsers) {
                this.deleteSubscriptionListTypeByUser(userId);
            }
        }
    }

    public void deleteSubscriptionListTypeByUser(String userId) {
        List<Subscription> userLocationSubscriptions =
            subscriptionRepository.findLocationSubscriptionsByUserId(userId);

        if (userLocationSubscriptions.isEmpty()) {
            Optional<SubscriptionListType> subscriptionListType =
                subscriptionListTypeRepository.findByUserId(userId);
            subscriptionListType.ifPresent(subscriptionListTypeRepository::delete);
        }
    }

    public String deleteAllSubscriptionsWithLocationNamePrefix(String prefix) {
        List<UUID> subscriptionIds = subscriptionRepository.findAllByLocationNameStartingWithIgnoreCase(prefix).stream()
            .map(Subscription::getId)
            .toList();

        if (!subscriptionIds.isEmpty()) {
            subscriptionRepository.deleteByIdIn(subscriptionIds);
        }
        return String.format("%s subscription(s) deleted for location name starting with %s",
                             subscriptionIds.size(), prefix);
    }

    private void notifySubscriberAboutSubscriptionDeletion(List<Subscription> locationSubscriptions,
                                                           String locationId) {
        List<String> userEmails = getUserEmailsForAllSubscriptions(locationSubscriptions);
        publicationService.sendLocationDeletionSubscriptionEmail(userEmails, locationId);
    }

    private void notifySystemAdminAboutSubscriptionDeletion(String userId, String additionalDetails) {
        PiUser piUser = accountService.getUserById(UUID.fromString(userId));
        if (piUser != null) {
            List<PiUser> systemAdmins = userRepository.findByRoles(SYSTEM_ADMIN);
            List<String> systemAdminEmails = systemAdmins.stream().map(PiUser::getEmail).toList();
            publicationService.sendSystemAdminEmail(systemAdminEmails, piUser.getEmail(),
                                                    ActionResult.SUCCEEDED, additionalDetails);
        } else {
            log.error(writeLog(String.format("User %s not found in the system when notifying system admins", userId)));
        }
    }

    private List<String> getUserEmailsForAllSubscriptions(List<Subscription> subscriptions) {
        List<String> userIds = subscriptions.stream()
            .map(Subscription::getUserId).toList();
        Map<String, Optional<String>> usersInfo = accountService.findUserEmailsByIds(userIds);

        List<String> userEmails = new ArrayList<>();

        usersInfo.forEach((userId, email) ->
            userEmails.add(
                email.isPresent() ? email.get() : ""
            )
        );
        userEmails.removeAll(Arrays.asList(""));
        return userEmails;
    }
}
