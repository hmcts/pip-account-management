package uk.gov.hmcts.reform.pip.account.management.service;


import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.BulkSubscriptionsSummary;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionsSummary;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionsSummaryDetails;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.subscription.LocationSubscriptionDeletion;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscription;
import uk.gov.hmcts.reform.pip.model.subscription.ThirdPartySubscriptionArtefact;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;
import uk.gov.hmcts.reform.pip.model.system.admin.DeleteLocationSubscriptionAction;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Service to communicate with publication-services microservice and send appropriate emails via govnotify on the
 * creation of a new administrator account.
 */
@Slf4j
@Service
@SuppressWarnings({"PMD.LooseCoupling", "PMD.TooManyMethods"})
public class PublicationService {

    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String NOTIFY_SUBSCRIPTION_PATH = "notify/subscription";
    private static final String NOTIFY_API_PATH = "notify/api";
    private static final String NOTIFY_LOCATION_SUBSCRIPTION_PATH = "notify/location-subscription-delete";

    private static final String EMAIL = "email";
    private static final String FULL_NAME = "fullName";
    private static final String USER_PROVENANCE = "userProvenance";
    private static final String LAST_SIGNED_IN_DATE = "lastSignedInDate";

    private final WebClient webClient;

    @Value("${service-to-service.publication-services}")
    private String url;

    @Autowired
    public PublicationService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Method which sends a request to the publication-services microservice to send a media account rejection email.
     *
     * @param mediaApplication - MediaApplication object containing applicant details
     * @param reasons          - reasons for rejection
     */
    public void sendMediaAccountRejectionEmail(MediaApplication mediaApplication,
                                                  Map<String, List<String>> reasons) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("applicantId", mediaApplication.getId().toString());
        jsonObject.put(FULL_NAME, mediaApplication.getFullName());
        jsonObject.put(EMAIL, mediaApplication.getEmail());
        jsonObject.put("reasons", reasons);
        try {
            webClient.post().uri(url + "/notify/media/reject")
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Media account rejection email failed to send with error: %s", ex.getMessage())
            ));
        }
    }

    public boolean sendNotificationEmailForDuplicateMediaAccount(String emailAddress, String fullName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMAIL, emailAddress);
        jsonObject.put(FULL_NAME, fullName);
        try {
            webClient.post().uri(url + "/notify/duplicate/media")
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Duplicate media account email failed to send with error: %s", ex.getMessage())
            ));
            return false;
        }
    }

    /**
     * Method calling Publication services send welcome email api.
     * @param emailAddress email address to send welcome email to
     * @param fullName full Name of the person to send welcome email to
     * @param isExisting bool to determine if coming from migration or new user creation
     * @return success message for logging
     */
    public boolean sendMediaNotificationEmail(String emailAddress, String fullName,
                                              boolean isExisting) {
        JSONObject body = new JSONObject();
        body.put(EMAIL, emailAddress);
        body.put(FULL_NAME, fullName);
        body.put("isExisting", isExisting);
        try {
            webClient.post().uri(url + WELCOME_EMAIL_URL)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (WebClientResponseException ex) {
            log.error(writeLog(
                String.format("Media account welcome email failed to send with error: %s", ex.getMessage())
            ));
            return false;
        }
    }

    /**
     * Method which sends a request to publication services to email the P&I team.
     * With a list of all media applications in the database.
     */
    public void sendMediaApplicationReportingEmail(List<MediaApplication> mediaApplicationList) {
        try {
            webClient.post().uri(url + "/notify/media/report")
                .body(BodyInserters.fromValue(mediaApplicationList)).retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Media application reporting email failed to send with error: %s", ex.getMessage())
            ));
        }
    }

    /**
     * Method which sends a request to publication services to email the user an account verification link.
     * @param emailAddress The media users email
     * @param fullName The full name of the media user to email
     */
    public void sendAccountVerificationEmail(String emailAddress, String fullName) {
        try {
            JSONObject body = new JSONObject();
            body.put(EMAIL, emailAddress);
            body.put(FULL_NAME, fullName);
            webClient.post().uri(url + "/notify/media/verification")
                .body(BodyInserters.fromValue(body)).retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Media account verification email failed to send with error: %s", ex.getMessage())
            ));
        }
    }

    public void sendInactiveAccountSignInNotificationEmail(String emailAddress, String fullName,
                                                           UserProvenances userProvenances, String lastSignedInDate) {
        try {
            JSONObject body = new JSONObject();
            body.put(EMAIL, emailAddress);
            body.put(FULL_NAME, fullName);
            body.put(USER_PROVENANCE, userProvenances.toString());
            body.put(LAST_SIGNED_IN_DATE, lastSignedInDate);
            webClient.post().uri(url + "/notify/user/sign-in")
                .body(BodyInserters.fromValue(body)).retrieve()
                .bodyToMono(String.class)
                .block();
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Inactive user sign-in notification email failed to send with error: %s",
                              ex.getMessage())
             ));
        }
    }

    /**
     * Publishing of the system admin account action.
     * @param systemAdminAction The system admin account action to publish.
     */
    public void sendSystemAdminAccountAction(SystemAdminAction systemAdminAction) {
        try {
            webClient.post().uri(url + "/notify/sysadmin/update")
                .body(BodyInserters.fromValue(systemAdminAction)).retrieve()
                .bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Publishing of system admin account action failed with error: %s", ex.getMessage())
            ));
        }
    }

    public void postSubscriptionSummaries(UUID artefactId, Map<String, List<Subscription>> subscriptions) {
        BulkSubscriptionsSummary payload = formatSubscriptionsSummary(artefactId, subscriptions);
        try {
            webClient.post().uri(url + "/" + NOTIFY_SUBSCRIPTION_PATH)
                .body(BodyInserters.fromValue(payload)).retrieve()
                .bodyToMono(Void.class)
                .block();

        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Subscription email failed to send with error: %s", ex.getMessage())
            ));
        }
    }

    public void sendThirdPartyList(ThirdPartySubscription subscriptions) {
        try {
            webClient.post().uri(url + "/" + NOTIFY_API_PATH)
                .bodyValue(subscriptions).retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (WebClientResponseException ex) {
            log.error(writeLog(
                String.format("Publication to third party failed to send with error: %s",
                              ex.getResponseBodyAsString())
            ));
        }
    }

    public void sendEmptyArtefact(ThirdPartySubscriptionArtefact subscriptionArtefact) {
        try {
            webClient.put().uri(url + "/" + NOTIFY_API_PATH)
                .bodyValue(subscriptionArtefact).retrieve()
                .bodyToMono(Void.class)
                .block();
        } catch (WebClientResponseException ex) {
            log.error(writeLog(
                String.format("Deleted artefact notification to third party failed to send with error: %s",
                              ex.getResponseBodyAsString())
            ));
        }
    }

    public void sendLocationDeletionSubscriptionEmail(List<String> emails, String locationId) {
        LocationSubscriptionDeletion payload = formatLocationSubscriptionDeletion(emails, locationId);
        try {
            webClient.post().uri(url + "/" + NOTIFY_LOCATION_SUBSCRIPTION_PATH)
                .body(BodyInserters.fromValue(payload)).retrieve()
                .bodyToMono(Void.class)
                .block();

        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Location deletion notification email failed to send with error: %s",
                              ex.getMessage())
            ));
        }
    }

    public void sendSystemAdminEmail(List<String> emails, String requesterEmail, ActionResult actionResult,
                                     String additionalDetails) {
        DeleteLocationSubscriptionAction payload =
            formatSystemAdminAction(emails, requesterEmail, actionResult, additionalDetails);
        try {
            webClient.post().uri(url + "/notify/sysadmin/update")
                .body(BodyInserters.fromValue(payload))
                .retrieve().bodyToMono(String.class)
                .block();

        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("System admin notification email failed to send with error: %s",
                              ex.getMessage())
            ));
        }
    }

    private DeleteLocationSubscriptionAction formatSystemAdminAction(List<String> emails, String requesterEmail,
                                                                     ActionResult actionResult,
                                                                     String additionalDetails) {
        DeleteLocationSubscriptionAction systemAdminAction = new DeleteLocationSubscriptionAction();
        systemAdminAction.setEmailList(emails);
        systemAdminAction.setRequesterEmail(requesterEmail);
        systemAdminAction.setActionResult(actionResult);
        systemAdminAction.setDetailString(additionalDetails);
        return systemAdminAction;
    }

    /**
     * Process data to form a subscriptions summary model which can be sent to publication services.
     *
     * @param artefactId The artefact id associated with the list of subscriptions
     * @param subscriptions A map containing each email which matches the criteria, alongside the subscriptions.
     * @return A subscriptions summary model
     */
    private BulkSubscriptionsSummary formatSubscriptionsSummary(UUID artefactId,
                                                                Map<String, List<Subscription>> subscriptions) {

        BulkSubscriptionsSummary bulkSubscriptionsSummary = new BulkSubscriptionsSummary();
        bulkSubscriptionsSummary.setArtefactId(artefactId);

        subscriptions.forEach((email, listOfSubscriptions) -> {
            SubscriptionsSummaryDetails subscriptionsSummaryDetails = new SubscriptionsSummaryDetails();
            listOfSubscriptions.forEach(subscription -> {
                switch (subscription.getSearchType()) {
                    case CASE_URN -> subscriptionsSummaryDetails.addToCaseUrn(subscription.getSearchValue());
                    case CASE_ID -> subscriptionsSummaryDetails.addToCaseNumber(subscription.getSearchValue());
                    case LOCATION_ID -> subscriptionsSummaryDetails.addToLocationId(subscription.getSearchValue());
                    default -> log.error(writeLog(
                        String.format("Search type was not one of allowed options: %s", subscription.getSearchType())
                    ));
                }
            });

            SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
            subscriptionsSummary.setEmail(email);
            subscriptionsSummary.setSubscriptions(subscriptionsSummaryDetails);

            bulkSubscriptionsSummary.addSubscriptionEmail(subscriptionsSummary);
        });

        return bulkSubscriptionsSummary;
    }

    private LocationSubscriptionDeletion formatLocationSubscriptionDeletion(
        List<String> emails, String locationId) {
        LocationSubscriptionDeletion locationSubscriptionDeletion = new LocationSubscriptionDeletion();
        locationSubscriptionDeletion.setLocationId(locationId);
        locationSubscriptionDeletion.setSubscriberEmails(emails);
        return locationSubscriptionDeletion;
    }
}
