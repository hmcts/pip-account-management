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
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.system.admin.SystemAdminAction;

import java.util.List;

@Slf4j
@Service
/**
 * Service to communicate with publication-services microservice and send appropriate emails via govnotify on the
 * creation of a new administrator account.
 */
public class PublicationService {

    @Value("${service-to-service.publication-services}")
    private String url;

    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";
    private static final String EMAIL = "email";
    private static final String FULL_NAME = "fullName";
    private static final String USER_PROVENANCE = "userProvenance";
    private static final String LAST_SIGNED_IN_DATE = "lastSignedInDate";

    @Autowired
    WebClient webClient;

    /**
     * Method which sends a request to the publication-services microservice which will send an email to the user
     * upon creation of a new admin account.
     * @param emailAddress - email address
     * @param forename - forename
     * @param surname - surname
     * @return string for logging success or failure
     */
    public boolean sendNotificationEmail(String emailAddress, String forename, String surname) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMAIL, emailAddress);
        jsonObject.put("forename", forename);
        jsonObject.put("surname", surname);
        try {
            log.info(webClient.post().uri(url + "/notify/created/admin")
                         .body(BodyInserters.fromValue(jsonObject)).retrieve()
                         .bodyToMono(String.class).block());
            return true;
        } catch (WebClientException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return false;
        }
    }

    public boolean sendNotificationEmailForDuplicateMediaAccount(String emailAddress, String fullName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EMAIL, emailAddress);
        jsonObject.put(FULL_NAME, fullName);
        try {
            log.info(webClient.post().uri(url + "/notify/duplicate/media")
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class).block());
            return true;
        } catch (WebClientException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return false;
        }
    }

    /**
     * Method calling Publication services send welcome email api.
     * @param emailAddress email address to send welcome email to
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
            log.info(webClient.post().uri(url + WELCOME_EMAIL_URL)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class).block());
            return true;
        } catch (WebClientResponseException ex) {
            log.error("Request to publication services {} failed due to: {}", WELCOME_EMAIL_URL, ex.getMessage());
            return false;
        }
    }

    /**
     * Method which sends a request to publication services to email the P&I team.
     * With a list of all media applications in the database.
     *
     * @return String for logging success or failure
     */
    public String sendMediaApplicationReportingEmail(List<MediaApplication> mediaApplicationList) {
        try {
            return webClient.post().uri(url + "/notify/media/report")
                .body(BodyInserters.fromValue(mediaApplicationList)).retrieve()
                .bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            return String.format("Email request failed to send with list of applications: %s. With error message: %s",
                                 mediaApplicationList, ex.getMessage());
        }
    }

    /**
     * Method which sends a request to publication services to email the user an account verification link.
     * @param emailAddress The media users email
     * @param fullName The full name of the media user to email
     */
    public String sendAccountVerificationEmail(String emailAddress, String fullName) {
        try {
            JSONObject body = new JSONObject();
            body.put(EMAIL, emailAddress);
            body.put(FULL_NAME, fullName);
            return webClient.post().uri(url + "/notify/media/verification")
                .body(BodyInserters.fromValue(body)).retrieve()
                .bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            return String.format("Media account verification email failed to send with error: %s", ex.getMessage());
        }
    }

    public String sendInactiveAccountSignInNotificationEmail(String emailAddress, String fullName,
                                                             UserProvenances userProvenances, String lastSignedInDate) {
        try {
            JSONObject body = new JSONObject();
            body.put(EMAIL, emailAddress);
            body.put(FULL_NAME, fullName);
            body.put(USER_PROVENANCE, userProvenances.toString());
            body.put(LAST_SIGNED_IN_DATE, lastSignedInDate);
            return webClient.post().uri(url + "/notify/user/sign-in")
                .body(BodyInserters.fromValue(body)).retrieve()
                .bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            return String.format("Inactive user sign-in notification email failed to send with error: %s",
                                 ex.getMessage());
        }
    }

    /**
     * Publishing of the system admin account action.
     * @param systemAdminAction The system admin account action to publish.
     * @return A string of the email ID that was sent, or the error message.
     */
    public String sendSystemAdminAccountAction(SystemAdminAction systemAdminAction) {
        try {
            return webClient.post().uri(url + "/notify/sysadmin/update")
                .body(BodyInserters.fromValue(systemAdminAction)).retrieve()
                .bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            return String.format("Publishing of system admin account action failed with error: %s",
                                 ex.getMessage());
        }
    }


}
