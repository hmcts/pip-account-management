package uk.gov.hmcts.reform.pip.account.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configures the Web Client that is used in requests to external services.
 */
@Configuration
@Profile({"!test", "!non-async"})
@EnableAsync
public class WebClientConfig {
    @Bean
    @Profile("!dev")
    public OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository clients) {
        OAuth2AuthorizedClientService service = new InMemoryOAuth2AuthorizedClientService(clients);
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
            new AuthorizedClientServiceOAuth2AuthorizedClientManager(clients, service);

        OAuth2AuthorizedClientProvider authorizedClientProvider =
            OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        manager.setAuthorizedClientProvider(authorizedClientProvider);

        return manager;
    }

    @Bean
    @Profile("!dev")
    WebClient webClient(OAuth2AuthorizedClientManager authorizedClientManager) {
        return WebClient.builder()
            .filter((request, next) -> next.exchange(withBearerToken(request, authorizedClientManager)))
            .build();
    }

    private static ClientRequest withBearerToken(ClientRequest request,
                                                 OAuth2AuthorizedClientManager authorizedClientManager) {
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
            .withClientRegistrationId("publicationServicesApi")
            .principal("pip-account-management")
            .build();
        OAuth2AuthorizedClient authorizedClient = authorizedClientManager.authorize(authorizeRequest);

        if (authorizedClient == null) {
            return request;
        }

        return ClientRequest.from(request)
            .headers(headers -> headers.setBearerAuth(authorizedClient.getAccessToken().getTokenValue()))
            .build();
    }

    @Bean
    @Profile("dev")
    WebClient webClientInsecure() {
        return WebClient.builder().build();
    }
}
