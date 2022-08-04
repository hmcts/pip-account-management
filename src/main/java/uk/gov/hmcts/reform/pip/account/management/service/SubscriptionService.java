package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

@Slf4j
@Component
/**
 * Service to communicate with subscription management microservice.
 */
public class SubscriptionService {

    @Value("${service-to-service.subscription-management}")
    private String url;

    @Autowired
    WebClient webClient;

    /**
     * Method which sends a request to subscription management to delete all subscriptions for a user.
     *
     * @param userId The id of the user to delete the subscriptions for.
     * @return string logging for success or failure.
     */
    public String sendSubscriptionDeletionRequest(String userId) {
        try {
            return webClient.delete().uri(url + "/subscription/user/" + userId)
                .retrieve().bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            return String.format("Deletion request to subscription management failed with error %s",
                                 ex.getMessage());
        }
    }
}
