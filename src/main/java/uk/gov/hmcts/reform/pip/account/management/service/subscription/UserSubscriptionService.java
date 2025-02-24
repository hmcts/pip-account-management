package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.CaseSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.ListTypeSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.LocationSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.UserSubscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class UserSubscriptionService {
    private final SubscriptionRepository subscriptionRepository;

    private final SubscriptionListTypeRepository subscriptionListTypeRepository;

    @Autowired
    public UserSubscriptionService(SubscriptionRepository subscriptionRepository,
                                   SubscriptionListTypeRepository subscriptionListTypeRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
    }

    /**
     * Find all subscriptions for a given user.
     * @param userId The user id to find the subscriptions for.
     * @return The list of subscriptions that have been found.
     */
    public UserSubscription findByUserId(UUID userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return new UserSubscription();
        }
        return collectSubscriptions(subscriptions);
    }

    /**
     * Delete all subscriptions by the user id.
     * @param userId The user id to delete the subscriptions from.
     * @return A confirmation message.
     */
    public String deleteAllByUserId(UUID userId) {
        subscriptionListTypeRepository.deleteByUserId(userId);
        subscriptionRepository.deleteAllByUserId(userId);
        String message = String.format("All subscriptions deleted for user id %s", userId);
        log.info(writeLog(message));
        return message;
    }

    private UserSubscription collectSubscriptions(List<Subscription> subscriptions) {
        UserSubscription userSubscription = new UserSubscription();
        subscriptions.forEach(subscription -> {
            switch (subscription.getSearchType()) {
                case LOCATION_ID -> {
                    LocationSubscription locationSubscription = new LocationSubscription();
                    locationSubscription.setSubscriptionId(subscription.getId());
                    locationSubscription.setLocationName(subscription.getLocationName());
                    locationSubscription.setLocationId(subscription.getSearchValue());
                    Optional<SubscriptionListType> subscriptionListType = subscriptionListTypeRepository
                        .findByUserId(subscription.getUserId());
                    if (subscriptionListType.isPresent()) {
                        locationSubscription.setListType(subscriptionListType.get().getListType());
                        locationSubscription.setListLanguage(subscriptionListType.get().getListLanguage());
                    }
                    locationSubscription.setDateAdded(subscription.getCreatedDate());
                    userSubscription.getLocationSubscriptions().add(locationSubscription);
                }
                case LIST_TYPE -> {
                    ListTypeSubscription listTypeSubscription = new ListTypeSubscription();
                    listTypeSubscription.setSubscriptionId(subscription.getId());
                    listTypeSubscription.setListType(subscription.getSearchValue());
                    listTypeSubscription.setDateAdded(subscription.getCreatedDate());
                    listTypeSubscription.setChannel(subscription.getChannel());
                    userSubscription.getListTypeSubscriptions().add(listTypeSubscription);
                }
                case CASE_ID, CASE_URN -> {
                    CaseSubscription caseSubscription = new CaseSubscription();
                    caseSubscription.setCaseName(subscription.getCaseName());
                    caseSubscription.setSubscriptionId(subscription.getId());
                    caseSubscription.setCaseNumber(subscription.getCaseNumber());
                    caseSubscription.setUrn(subscription.getUrn());
                    caseSubscription.setPartyNames(subscription.getPartyNames());
                    caseSubscription.setSearchType(subscription.getSearchType());
                    caseSubscription.setDateAdded(subscription.getCreatedDate());
                    userSubscription.getCaseSubscriptions().add(caseSubscription);
                }
                default -> { // No default case
                }
            }
        });
        return userSubscription;
    }
}
