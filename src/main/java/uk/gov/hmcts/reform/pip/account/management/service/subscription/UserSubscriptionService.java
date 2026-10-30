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
    public UserSubscription findByUserIdV2(UUID userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        if (subscriptions.isEmpty()) {
            return new UserSubscription();
        }
        return collectSubscriptionsV2(subscriptions);
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

    private UserSubscription collectSubscriptionsV2(List<Subscription> subscriptions) {
        UserSubscription userSubscription = new UserSubscription();
        subscriptions.forEach(subscription -> {
            switch (subscription.getSearchType()) {
                case LOCATION_ID ->
                    userSubscription.getLocationSubscriptions()
                        .add(configureLocationSubscription(subscription));
                case LIST_TYPE ->
                    userSubscription.getListTypeSubscriptions()
                        .add(configureListTypeSubscription(subscription));
                case CASE_NUMBER, CASE_NAME ->
                    userSubscription.getCaseSubscriptions()
                        .add(configureCaseSubscriptionV2(subscription));
                default -> { // No default case
                }
            }
        });
        return userSubscription;
    }

    private LocationSubscription configureLocationSubscription(Subscription subscription) {
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

        return locationSubscription;
    }

    private ListTypeSubscription configureListTypeSubscription(Subscription subscription) {
        ListTypeSubscription listTypeSubscription = new ListTypeSubscription();
        listTypeSubscription.setSubscriptionId(subscription.getId());
        listTypeSubscription.setListType(subscription.getSearchValue());
        listTypeSubscription.setDateAdded(subscription.getCreatedDate());
        listTypeSubscription.setChannel(subscription.getChannel());
        return listTypeSubscription;
    }

    private CaseSubscription configureCaseSubscriptionV2(Subscription subscription) {
        CaseSubscription caseSubscription = new CaseSubscription();
        caseSubscription.setCaseName(subscription.getCaseName());
        caseSubscription.setSubscriptionId(subscription.getId());
        caseSubscription.setCaseNumber(subscription.getCaseNumber());
        caseSubscription.setSearchType(subscription.getSearchType());
        caseSubscription.setDateAdded(subscription.getCreatedDate());
        return caseSubscription;
    }
}
