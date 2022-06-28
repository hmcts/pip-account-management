package uk.gov.hmcts.reform.pip.account.management.service;


import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;

import java.util.List;

@Slf4j
@Component
/**
 * Service to communicate with publication-services microservice and send appropriate emails via govnotify on the
 * creation of a new administrator account.
 */
public class PublicationService {

    @Value("${service-to-service.publication-services}")
    private String url;

    private static final String WELCOME_EMAIL_URL = "/notify/welcome-email";

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
        jsonObject.put("email", emailAddress);
        jsonObject.put("forename", forename);
        jsonObject.put("surname", surname);
        try {
            return webClient.post().uri(url + "/notify/created/admin")
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class).block();

        } catch (WebClientException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return false;
        }
    }

    public String sendNotificationEmailForSetupMediaAccount(String emailAddress, String fullName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", emailAddress);
        jsonObject.put("isExisting", false);
        jsonObject.put("fullName", fullName);
        try {
            return webClient.post().uri(url + "/notify/welcome-email")
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class).block();

        } catch (WebClientException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return "Email request failed to send: " + emailAddress;
        }
    }

    public String sendNotificationEmailForDuplicateMediaAccount(String emailAddress, String fullName) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", emailAddress);
        jsonObject.put("fullName", fullName);
        try {
            return webClient.post().uri(url + "/notify/duplicate/media")
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return "Email request failed to send: " + emailAddress;
        }
    }

    /**
     * Method calling Publication services send welcome email api.
     * @param emailAddress email address to send welcome email to
     * @param isExisting bool to determine if coming from migration or new user creation
     * @return success message for logging
     */
    public boolean sendMediaNotificationEmail(String emailAddress, boolean isExisting) {
        JSONObject body = new JSONObject();
        body.put("email", emailAddress);
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
}
