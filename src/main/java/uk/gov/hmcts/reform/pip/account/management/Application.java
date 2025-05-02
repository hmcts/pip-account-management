package uk.gov.hmcts.reform.pip.account.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import uk.gov.hmcts.reform.pip.account.management.config.AzureBlobConfigurationProperties;
import uk.gov.hmcts.reform.pip.account.management.config.ThirdPartyApiConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
    AzureBlobConfigurationProperties.class,
    ThirdPartyApiConfigurationProperties.class
})
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, its not a utility class
@EnableScheduling
public class Application {
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
