package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.config.ThirdPartyApiConfigurationProperties;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@Slf4j
public class SubscriptionChannelService {
    private final AccountService accountService;
    private final ThirdPartyApiConfigurationProperties thirdPartyApi;

    @Autowired
    public SubscriptionChannelService(AccountService accountService,
                                      ThirdPartyApiConfigurationProperties thirdPartyApi) {
        this.accountService = accountService;
        this.thirdPartyApi = thirdPartyApi;
    }

    /**
     * Returns a list of channels associated with the subscription.
     * @return The list of channels.
     */
    public List<Channel> retrieveChannels() {
        return List.of(Channel.values());
    }

    /**
     * Parent method which handles the subscription flow, initially capturing duplicate users, then matching user
     * ids to emails and building the final map of individual user emails to relevant subscription objects.
     * @param listOfSubs - a list of subscription objects associated with a publication
     * @return A map of user emails to list of subscriptions
     */
    public Map<String, List<Subscription>> buildEmailSubscriptions(List<Subscription> listOfSubs) {
        Map<String, List<Subscription>> mappedSubscriptions = deduplicateSubscriptions(listOfSubs);

        List<String> userIds = new ArrayList<>(mappedSubscriptions.keySet());
        Map<String, String> mapOfUsersAndEmails = accountService.findUserEmailsByIds(userIds);

        if (mapOfUsersAndEmails.isEmpty()) {
            log.error(writeLog("No email channel found for any of the users provided"));
            return Collections.emptyMap();
        }
        return userIdToUserEmailSwitcher(mappedSubscriptions, mapOfUsersAndEmails);
    }

    /**
     * Creates map of third party api urls and a list of Subscriptions associated with them.
     * @param subscriptions list of subscriptions to be trimmed of duplications and associated with an api.
     * @return Map of Url to list of subscriptions.
     */
    public Map<String, List<Subscription>> buildApiSubscriptions(List<Subscription> subscriptions) {
        return userIdToApiValueSwitcher(deduplicateSubscriptions(subscriptions));
    }

    /**
     * This method accesses the list of subscriptions passed in, and transforms it into a list of user id strings
     * with associated subscriptions for each.
     * @param listOfSubs - a list of subscriptions for a given object.
     */
    Map<String, List<Subscription>> deduplicateSubscriptions(List<Subscription> listOfSubs) {
        Map<String, List<Subscription>> mapOfSubscriptions = new ConcurrentHashMap<>();
        listOfSubs.forEach(
            subscription -> mapOfSubscriptions.computeIfAbsent(subscription.getUserId(), x -> new ArrayList<>())
                .add(subscription)
        );
        return mapOfSubscriptions;
    }

    /**
     * Handle the flipping of userId to email as the key for the map.
     * @param userIdMap - A map of userIds to the list of subscription objects associated with them.
     * @param userEmailMap - a map of userIds to their email addresses.
     * @return Map of email addresses to subscription objects.
     */
    Map<String, List<Subscription>> userIdToUserEmailSwitcher(Map<String, List<Subscription>> userIdMap,
                                                              Map<String, String> userEmailMap) {
        Map<String, List<Subscription>> cloneMap = new ConcurrentHashMap<>(userIdMap);

        cloneMap.forEach((userId, subscriptions) -> {
            userIdMap.put(userEmailMap.get(userId), subscriptions);
            userIdMap.remove(userId);
        });
        return userIdMap;
    }

    /**
     * Takes in Map and replaces the user id that's not needed for third party, to the URL the subscription needs to
     * be sent to.
     * @param subscriptions Map of user id's to list of subscriptions.
     * @return Map of URL's to list of subscriptions.
     */
    private Map<String, List<Subscription>> userIdToApiValueSwitcher(Map<String, List<Subscription>> subscriptions) {
        AtomicBoolean invalidChannel = new AtomicBoolean(false);
        Map<String, List<Subscription>> switchedMap = new ConcurrentHashMap<>();
        subscriptions.forEach((recipient, subscriptionList)  -> {
            if (Channel.API_COURTEL.equals(subscriptionList.get(0).getChannel())) {
                switchedMap.put(thirdPartyApi.getCourtel(), subscriptionList);
            } else {
                log.error(writeLog("Invalid channel for API subscriptions: "
                                       + subscriptionList.get(0).getChannel()));
                invalidChannel.set(true);
            }
        });

        return invalidChannel.get() ? Collections.emptyMap() : switchedMap;
    }
}
