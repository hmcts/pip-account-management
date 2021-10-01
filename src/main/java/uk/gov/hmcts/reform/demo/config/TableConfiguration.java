package uk.gov.hmcts.reform.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "azure.table")
@Getter
@Setter
public class TableConfiguration {

    private String connectionString;

    private String tableName;

}
