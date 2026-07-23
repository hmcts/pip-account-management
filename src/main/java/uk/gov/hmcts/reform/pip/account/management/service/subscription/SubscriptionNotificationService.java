package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartySubscriptionNotificationService;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;
import uk.gov.hmcts.reform.pip.model.publication.ArtefactCaseInfo;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.LegacyThirdPartySubscriptionArtefact;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.publication.Sensitivity.CLASSIFIED;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.CASE_ID;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.CASE_NAME;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.CASE_NUMBER;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.CASE_URN;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.LIST_TYPE;

@Service
@Slf4j
public class SubscriptionNotificationService {
    @Deprecated private static final String CASE_NUMBER_KEY = "caseNumber";
    @Deprecated private static final String CASE_URN_KEY = "caseUrn";

    private static final String SUBSCRIBER_NOTIFICATION_LOG = "Summary being sent to publication services for id ";

    private final SubscriptionRepository repository;

    private final SubscriptionChannelService subscriptionChannelService;

    private final AccountService accountService;

    private final PublicationService publicationService;

    private final ThirdPartySubscriptionNotificationService thirdPartySubscriptionNotificationService;

    @Autowired
    public SubscriptionNotificationService(
        SubscriptionRepository repository,
        SubscriptionChannelService subscriptionChannelService,
        AccountService accountService,
        PublicationService publicationService,
        ThirdPartySubscriptionNotificationService thirdPartySubscriptionNotificationService
    ) {
        this.repository = repository;
        this.subscriptionChannelService = subscriptionChannelService;
        this.accountService = accountService;
        this.publicationService = publicationService;
        this.thirdPartySubscriptionNotificationService = thirdPartySubscriptionNotificationService;
    }

    /**
     * Collect all subscribers for the artefact, and handle sending of email and third party subscriptions to
     * the subscribers.
     * @param artefact the artefact to collect the subscriptions for.
     */
    @Async
    @Deprecated
    // Method to be removed after related data-management changes merged to use the more specific endpoints
    // for email and API subscribers
    public void collectSubscribers(Artefact artefact) {

        List<Subscription> subscriptionList = new ArrayList<>(querySubscriptionValueForLocation(
            artefact.getLocationId(), artefact.getListType().toString(),
            artefact.getLanguage().toString()));

        subscriptionList.addAll(querySubscriptionValue(LIST_TYPE.name(), artefact.getListType().name()));

        if (artefact.getSearch().containsKey("cases")) {
            artefact.getSearch().get("cases").forEach(object -> subscriptionList.addAll(extractSearchValue(object)));
        }

        List<Subscription> subscriptionsToContact = CLASSIFIED.equals(artefact.getSensitivity())
            ? validateSubscriptionPermissions(subscriptionList, artefact)
            : subscriptionList;

        handleSubscriptionSending(artefact.getArtefactId(), subscriptionsToContact);
        thirdPartySubscriptionNotificationService.handleThirdPartySubscription(artefact);
    }

    /**
     * Collect all email subscribers for the artefact, and handle sending of email to the subscribers.
     * @param artefact the artefact to collect the subscriptions for.
     */
    @Async
    @Deprecated
    public void collectEmailSubscribers(Artefact artefact) {
        List<Subscription> subscriptionList = new ArrayList<>(
            querySubscriptionValueForLocation(artefact.getLocationId(), artefact.getListType().toString(),
                                              artefact.getLanguage().toString())
        );

        if (artefact.getSearch().containsKey("cases")) {
            artefact.getSearch().get("cases").forEach(object -> subscriptionList.addAll(extractSearchValue(object)));
        }

        List<Subscription> subscriptionsToContact = CLASSIFIED.equals(artefact.getSensitivity())
            ? validateSubscriptionPermissions(subscriptionList, artefact)
            : subscriptionList;

        handleEmailSubscriptionSending(artefact.getArtefactId(), subscriptionsToContact);
    }

    /**
     * Collect all email subscribers for the artefact, and handle sending of email to the subscribers.
     * @param artefact the artefact to collect the subscriptions for.
     */
    @Async
    public void collectEmailSubscribersV2(Artefact artefact) {
        List<Subscription> subscriptionList = new ArrayList<>(
            querySubscriptionValueForLocation(artefact.getLocationId(), artefact.getListType().toString(),
                                              artefact.getLanguage().toString())
        );

        if (!CollectionUtils.isEmpty(artefact.getCaseInfoList())) {
            artefact.getCaseInfoList().forEach(
                caseInfo -> subscriptionList.addAll(extractCaseSubscriptions(caseInfo))
            );
        }

        List<Subscription> subscriptionsToContact = CLASSIFIED.equals(artefact.getSensitivity())
            ? validateSubscriptionPermissions(subscriptionList, artefact)
            : subscriptionList;

        handleEmailSubscriptionSendingV2(artefact, subscriptionsToContact);
    }

    /**
     * Collect all API subscribers for the artefact, and handle sending of third party subscriptions.
     * @param artefact the artefact to collect the subscriptions for.
     */
    @Async
    public void collectApiSubscribers(Artefact artefact) {
        List<Subscription> subscriptionList = new ArrayList<>(
            querySubscriptionValue(LIST_TYPE.name(), artefact.getListType().name())
        );

        List<Subscription> subscriptionsToContact = CLASSIFIED.equals(artefact.getSensitivity())
            ? validateSubscriptionPermissions(subscriptionList, artefact)
            : subscriptionList;

        handleLegacyThirdPartySubscriptionSending(artefact.getArtefactId(), subscriptionsToContact);
        thirdPartySubscriptionNotificationService.handleThirdPartySubscription(artefact);
    }

    /**
     * Collect the third party subscribers for the deleted artefact, and handle sending of notification emails to them.
     * @param artefactBeingDeleted the artefact which has been deleted.
     */
    @Async
    public void collectThirdPartyForDeletion(Artefact artefactBeingDeleted) {
        List<Subscription> subscriptionList = new ArrayList<>(querySubscriptionValue(
            LIST_TYPE.name(), artefactBeingDeleted.getListType().name()));

        List<Subscription> subscriptionsToContact = CLASSIFIED.equals(artefactBeingDeleted.getSensitivity())
            ? validateSubscriptionPermissions(subscriptionList, artefactBeingDeleted)
            : subscriptionList;

        handleDeletedArtefactSending(subscriptionsToContact, artefactBeingDeleted);
        thirdPartySubscriptionNotificationService.handleThirdPartySubscriptionForDeletedPublication(
            artefactBeingDeleted
        );
    }

    private List<Subscription> validateSubscriptionPermissions(List<Subscription> subscriptions, Artefact artefact) {
        List<Subscription> filteredList = new ArrayList<>();
        subscriptions.forEach(subscription -> {
            if (Boolean.TRUE.equals(accountService.isUserAuthorisedForPublication(
                subscription.getUserId(), artefact.getListType(), artefact.getSensitivity()
            ))) {
                filteredList.add(subscription);
            }
        });
        return filteredList;
    }

    private List<Subscription> querySubscriptionValue(String term, String value) {
        return repository.findSubscriptionsBySearchValue(term, value);
    }

    private List<Subscription> querySubscriptionValueForLocation(String value, String listType,
                                                                 String listLanguage) {
        return repository.findSubscriptionsByLocationSearchValue(value, listType, listLanguage);
    }

    @SuppressWarnings("unchecked")
    @Deprecated
    private List<Subscription> extractSearchValue(Object caseObject) {
        List<Subscription> subscriptionList = new ArrayList<>();
        Map<String, Object> caseMap = (Map) caseObject;

        if (caseMap.containsKey(CASE_NUMBER_KEY) && caseMap.get(CASE_NUMBER_KEY) != null) {
            subscriptionList.addAll(querySubscriptionValue(CASE_ID.name(), caseMap.get(CASE_NUMBER_KEY).toString()));
        }

        if (caseMap.containsKey(CASE_URN_KEY) && caseMap.get(CASE_URN_KEY) != null) {
            subscriptionList.addAll(querySubscriptionValue(CASE_URN.name(), caseMap.get(CASE_URN_KEY).toString()));
        }

        if (!caseMap.containsKey(CASE_NUMBER_KEY) || !caseMap.containsKey(CASE_URN_KEY)) {
            log.warn(writeLog(String.format("No value found in %s for case number or urn", caseObject)));
        }
        return subscriptionList;
    }

    @SuppressWarnings("unchecked")
    private List<Subscription> extractCaseSubscriptions(ArtefactCaseInfo caseInfo) {
        List<Subscription> subscriptionList = new ArrayList<>();

        if (StringUtils.isNotEmpty(caseInfo.getCaseNumber())) {
            subscriptionList.addAll(querySubscriptionValue(CASE_NUMBER.name(), caseInfo.getCaseNumber()));
        } else if (StringUtils.isNotEmpty(caseInfo.getCaseName())) {
            // Case name subscription is only used if there is no case number, as the case name subscription is a
            // fallback for when the case number is not available
            subscriptionList.addAll(querySubscriptionValue(CASE_NAME.name(), caseInfo.getCaseName()));
        }

        return subscriptionList;
    }

    /**
     * Handle forming and sending of subscriptions to publication services.
     *
     * @param artefactId The id of the artefact being sent
     * @param subscriptionsList The list of subscriptions being sent
     */
    @Deprecated
    // Method to be removed after related data-management changes merged to use the more specific endpoints
    // for email and API subscribers
    private void handleSubscriptionSending(UUID artefactId, List<Subscription> subscriptionsList) {
        List<Subscription> emailList = sortSubscriptionByChannel(subscriptionsList, Channel.EMAIL.notificationRoute);
        List<Subscription> apiList = sortSubscriptionByChannel(subscriptionsList,
                                                               Channel.API_COURTEL.notificationRoute);

        Map<String, List<Subscription>> emailSubscriptions =
            subscriptionChannelService.buildEmailSubscriptions(emailList);
        if (!emailSubscriptions.isEmpty()) {
            log.info(writeLog(SUBSCRIBER_NOTIFICATION_LOG + artefactId));
            publicationService.postSubscriptionSummaries(artefactId, emailSubscriptions);
        }

        subscriptionChannelService.buildLegacyApiSubscriptions(apiList)
            .forEach((api, subscriptions) -> publicationService.legacySendThirdPartyList(
                new LegacyThirdPartySubscription(api, artefactId)
            ));
        log.info(writeLog(String.format("Collected %s api subscribers", apiList.size())));
    }

    /**
     * Handle forming and sending of subscriptions to publication services.
     *
     * @param artefactId The id of the artefact being sent
     * @param subscriptionsList The list of subscriptions being sent
     */
    @Deprecated
    private void handleEmailSubscriptionSending(UUID artefactId, List<Subscription> subscriptionsList) {
        List<Subscription> emailList = sortSubscriptionByChannel(subscriptionsList, Channel.EMAIL.notificationRoute);

        Map<String, List<Subscription>> emailSubscriptions = subscriptionChannelService
            .buildEmailSubscriptions(emailList);
        if (!emailSubscriptions.isEmpty()) {
            log.info(writeLog(SUBSCRIBER_NOTIFICATION_LOG + artefactId));
            publicationService.postSubscriptionSummaries(artefactId, emailSubscriptions);
        }
    }

    /**
     * Handle forming and sending of subscriptions to publication services.
     *
     * @param artefact The artefact being sent
     * @param subscriptionsList The list of subscriptions being sent
     */
    private void handleEmailSubscriptionSendingV2(Artefact artefact, List<Subscription> subscriptionsList) {
        List<Subscription> emailList = sortSubscriptionByChannel(subscriptionsList, Channel.EMAIL.notificationRoute);

        Map<String, List<Subscription>> emailSubscriptions = subscriptionChannelService
            .buildEmailSubscriptions(emailList);
        if (!emailSubscriptions.isEmpty()) {
            log.info(writeLog(SUBSCRIBER_NOTIFICATION_LOG + artefact.getArtefactId()));
            publicationService.postSubscriptionSummariesV2(artefact, emailSubscriptions);
        }
    }

    /**
     * Handle forming and sending of subscriptions to publication services.
     *
     * @param artefactId The id of the artefact being sent
     * @param subscriptionsList The list of subscriptions being sent
     */
    private void handleLegacyThirdPartySubscriptionSending(UUID artefactId, List<Subscription> subscriptionsList) {
        List<Subscription> apiList = sortSubscriptionByChannel(subscriptionsList,
                                                               Channel.API_COURTEL.notificationRoute);
        subscriptionChannelService.buildLegacyApiSubscriptions(apiList)
            .forEach((api, subscriptions) -> publicationService.legacySendThirdPartyList(
                new LegacyThirdPartySubscription(api, artefactId)
            ));
        log.info(writeLog(String.format("Collected %s api subscribers", apiList.size())));
    }

    /**
     * Create a list of subscriptions for the correct channel.
     *
     * @param subscriptionsList The list of subscriptions to sort through
     * @param channel The channel we want the subscriptions of
     * @return A list of subscriptions
     */
    private List<Subscription> sortSubscriptionByChannel(List<Subscription> subscriptionsList, String channel) {
        List<Subscription> sortedSubscriptionsList = new ArrayList<>();

        subscriptionsList.forEach((Subscription subscription) -> {
            if (channel.equals(subscription.getChannel().notificationRoute)) {
                sortedSubscriptionsList.add(subscription);
            }
        });

        return sortedSubscriptionsList;
    }

    private void handleDeletedArtefactSending(List<Subscription> subscriptions, Artefact artefactBeingDeleted) {
        List<Subscription> apiList = sortSubscriptionByChannel(subscriptions,
                                                               Channel.API_COURTEL.notificationRoute);
        subscriptionChannelService.buildLegacyApiSubscriptions(apiList)
            .forEach((api, subscription) -> publicationService.legacySendEmptyArtefact(
                new LegacyThirdPartySubscriptionArtefact(api, artefactBeingDeleted)
            ));
    }
}
