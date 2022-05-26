package uk.gov.hmcts.reform.pip.account.management.service;


import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;

import java.net.URI;
import java.net.URISyntaxException;
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
    public String sendNotificationEmail(String emailAddress, String forename, String surname) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", emailAddress);
        jsonObject.put("forename", forename);
        jsonObject.put("surname", surname);
        try {
            return webClient.post().uri(new URI(url + "/notify/created/admin"))
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class).block();

        } catch (WebClientException | URISyntaxException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return "Email request failed to send: " + emailAddress;
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
            log.error(String.format("Request: %s failed. With error message: %s",
                                    mediaApplicationList, ex.getMessage()));
            return String.format("Email request failed to send with list of applications: %s",
                                 mediaApplicationList);
        }
    }
}
