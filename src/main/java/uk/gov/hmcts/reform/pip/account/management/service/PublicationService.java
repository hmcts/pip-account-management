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
import java.util.Map;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

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
            webClient.post().uri(url + "/notify/created/admin")
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class)
                .block();
            return true;
        } catch (WebClientException ex) {
            log.error(writeLog(
                String.format("Admin account welcome email failed to send with error: %s", ex.getMessage())
            ));
            return false;
        }
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
}
