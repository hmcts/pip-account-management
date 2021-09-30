package uk.gov.hmcts.reform.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "azure.user")
@Getter
@Setter
public class UserConfiguration {

    private String tokenProvider;

    private String identityIssuer;

    private String passwordPolicy;



}
