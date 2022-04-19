package uk.gov.hmcts.reform.pip.account.management.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to store properties around configuring users.
 */
@Configuration
@ConfigurationProperties(prefix = "azure.user")
@Getter
@Setter
public class UserConfiguration {

    private String identityIssuer;

    private String signInType;

}
