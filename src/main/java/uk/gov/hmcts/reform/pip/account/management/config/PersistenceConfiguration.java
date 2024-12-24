package uk.gov.hmcts.reform.pip.account.management.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@Profile("!integration-basic")
public class PersistenceConfiguration {
}
