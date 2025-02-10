package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.LOCATION_ID;

/**
 * Service layer for dealing with subscriptions.
 */
@Slf4j
@Service
@SuppressWarnings("PMD.TooManyMethods")
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionListTypeService subscriptionListTypeService;
    private final SubscriptionLocationService subscriptionLocationService;

    @Autowired
    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               SubscriptionListTypeService subscriptionListTypeService,
                               SubscriptionLocationService subscriptionLocationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionListTypeService = subscriptionListTypeService;
        this.subscriptionLocationService = subscriptionLocationService;
    }

    public Subscription createSubscription(Subscription subscription, String actioningUserId) {
        log.info(writeLog(actioningUserId, UserActions.CREATE_SUBSCRIPTION,
                          subscription.getSearchType().toString()));

        duplicateSubscriptionHandler(subscription);
        subscription.setLastUpdatedDate(subscription.getCreatedDate());

        return subscriptionRepository.save(subscription);
    }

    public void deleteById(UUID id, String actioningUserId) {
        Subscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new SubscriptionNotFoundException(String.format(
                "No subscription found with the subscription id %s", id
            )));
        subscriptionRepository.deleteById(id);

        if (subscription.getSearchType().equals(LOCATION_ID)) {
            subscriptionLocationService.deleteSubscriptionListTypeByUser(subscription.getUserId());
        }

        log.info(writeLog(actioningUserId, UserActions.DELETE_SUBSCRIPTION,
                          id.toString()));
    }

    public void bulkDeleteSubscriptions(List<UUID> ids) {
        List<Subscription> subscriptions = subscriptionRepository.findByIdIn(ids);
        if (ids.size() > subscriptions.size()) {
            List<UUID> missingIds = new ArrayList<>(ids);
            missingIds.removeAll(subscriptions.stream()
                                     .map(Subscription::getId)
                                     .toList());
            throw new SubscriptionNotFoundException("No subscription found with the subscription ID(s): "
                    + missingIds.toString().replace("[", "").replace("]", ""));
        }

        //FIND ALL THE LOCATION SUBSCRIPTION FOR THE USER AND CHECK IF MORE THAN ONE LOCATION SUBSCRIPTION EXISTS
        //DO NOT DELETE RECORD FROM SUBSCRIPTION LIST TYPE BECAUSE ONE RECORD IS LINKED WITH ALL THE LOCATION
        //SUBSCRIPTIONS
        List<Subscription> bulkDeleteLocationSubscriptions = subscriptions.stream()
            .filter(s -> s.getSearchType()
            .equals(LOCATION_ID)).toList();

        List<Subscription> userLocationSubscriptions = subscriptionRepository
            .findLocationSubscriptionsByUserId(subscriptions.get(0).getUserId());

        if (!userLocationSubscriptions.isEmpty()
            && bulkDeleteLocationSubscriptions.size()
            == subscriptionRepository.findLocationSubscriptionsByUserId(subscriptions.get(0).getUserId()).size()) {
            subscriptionListTypeService.deleteListTypesForSubscription(subscriptions.get(0).getUserId());
        }

        subscriptionRepository.deleteByIdIn(ids);
        subscriptions.forEach(s -> log.info(writeLog(s.getUserId(), UserActions.DELETE_SUBSCRIPTION,
                                                     s.getId().toString())));
    }

    public List<Subscription> findAll() {
        return subscriptionRepository.findAll();
    }

    public Subscription findById(UUID subscriptionId) {
        Optional<Subscription> subscription = subscriptionRepository.findById(subscriptionId);
        if (subscription.isEmpty()) {
            throw new SubscriptionNotFoundException(String.format(
                "No subscription found with the subscription id %s",
                subscriptionId
            ));
        }
        return subscription.get();
    }

    /**
     * Take in a new user subscription and check if any with the same criteria already exist.
     * If it does then delete the original subscription as the new one will supersede it.
     *
     * @param subscription The new subscription that will be created
     */
    private void duplicateSubscriptionHandler(Subscription subscription) {
        subscriptionRepository.findByUserId(subscription.getUserId()).forEach(existingSub -> {
            if (existingSub.getSearchType().equals(subscription.getSearchType())
                && existingSub.getSearchValue().equals(subscription.getSearchValue())) {
                subscriptionRepository.delete(existingSub);
            }
        });
    }

    /**
     * Previous version of the MI Reporting service method. No longer used and soon to be removed.
     * @deprecated This method will be removed in the future in favour of the V2 equivalent.
     */
    @Deprecated(since = "2")
    public String getAllSubscriptionsDataForMiReporting() {
        StringBuilder builder = new StringBuilder(60);
        builder.append("id,channel,search_type,user_id,court_name,created_date").append(System.lineSeparator());
        subscriptionRepository.getAllSubsDataForMi()
            .forEach(line -> builder.append(line).append(System.lineSeparator()));
        return builder.toString();
    }

    public List<AllSubscriptionMiData> getAllSubscriptionsDataForMiReportingV2() {
        return subscriptionRepository.getAllSubsDataForMiV2();
    }

    /**
     * Previous version of the MI Reporting service method. No longer used and soon to be removed.
     * @deprecated This method will be removed in the future in favour of the V2 equivalent.
     */
    @Deprecated(since = "2")
    public String getLocalSubscriptionsDataForMiReporting() {
        StringBuilder builder = new StringBuilder(60);
        builder.append("id,search_value,channel,user_id,court_name,created_date").append(System.lineSeparator());
        subscriptionRepository.getLocalSubsDataForMi()
            .forEach(line -> builder.append(line).append(System.lineSeparator()));
        return builder.toString();
    }

    public List<LocationSubscriptionMiData> getLocationSubscriptionsDataForMiReportingV2() {
        return subscriptionRepository.getLocationSubsDataForMiV2();
    }
}
