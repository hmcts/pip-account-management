package uk.gov.hmcts.reform.pip.account.management.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;


@Profile({"test", "non-async"})
@Configuration
class WebClientTestConfiguration {

    @Bean
    public WebClient webClient() {
        return WebClient.create();
    }
}
