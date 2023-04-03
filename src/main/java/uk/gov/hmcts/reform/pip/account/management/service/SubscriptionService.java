package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;

import static org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction.clientRegistrationId;

@Slf4j
@Service
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
                .attributes(clientRegistrationId("subscriptionManagementApi"))
                .retrieve().bodyToMono(String.class).block();
        } catch (WebClientException ex) {
            return String.format("Deletion request to subscription management failed with error %s",
                                 ex.getMessage());
        }
    }
}
