package uk.gov.hmcts.reform.pip.account.management.model.thirdparty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiOauthConfigurationDto {
    private UUID userId;
    private String destinationUrl;
    private String tokenUrl;
}
