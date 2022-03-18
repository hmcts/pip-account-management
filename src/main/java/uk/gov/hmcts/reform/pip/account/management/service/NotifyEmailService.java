package uk.gov.hmcts.reform.pip.account.management.service;


import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Component
public class NotifyEmailService {
    @Value("${service-to-service.publication-services}")
    private String url;


    public String sendNotificationEmail(String emailData) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("email", emailData);
        jsonObject.put("isExisting", false);
        WebClient webClient = WebClient.create();
        log.info("Attempting to send email to " + url);
        try {
            String returnValue = webClient.post().uri(new URI(url + "/notify/welcome-email"))
                .body(BodyInserters.fromValue(jsonObject)).retrieve()
                .bodyToMono(String.class).block();
            log.info(String.format("Email trigger for %s sent to Publication-Services", emailData));
            return returnValue;

        } catch (WebClientException | URISyntaxException ex) {
            log.error(String.format("Request failed with error message: %s", ex.getMessage()));
            return "Email request failed to send: " + emailData;
        }
    }
}
