package uk.gov.hmcts.reform.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to store properties around configuring the Azure client.
 */
@Configuration
@ConfigurationProperties(prefix = "azure.id")
@Getter
@Setter
public class ClientConfiguration {

    private String clientId;

    private String clientSecret;

    private String tenantGuid;

    private String tokenProvider;

}
