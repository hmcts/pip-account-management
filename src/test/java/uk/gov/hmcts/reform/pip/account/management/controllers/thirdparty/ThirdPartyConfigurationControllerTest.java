package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfigurationDto;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartyConfigurationService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyConfigurationControllerTest {
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ThirdPartyConfigurationService thirdPartyConfigurationService;

    @InjectMocks
    private ThirdPartyConfigurationController controller;

    @Test
    void testCreateThirdPartyConfiguration() {
        ApiOauthConfigurationDto config = new ApiOauthConfigurationDto();
        config.setUserId(USER_ID);

        ResponseEntity<String> response = controller.createThirdPartyConfiguration(config, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be CREATED")
            .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
            .as("Response body should contain the user ID")
            .contains(USER_ID.toString());

        verify(thirdPartyConfigurationService).createThirdPartyConfiguration(config);
    }

    @Test
    void testGetThirdPartyConfiguration() {
        ApiOauthConfiguration config = new ApiOauthConfiguration();
        config.setUserId(USER_ID);

        when(thirdPartyConfigurationService.findThirdPartyConfigurationByUserId(USER_ID)).thenReturn(config);

        ResponseEntity<ApiOauthConfiguration> response = controller.getThirdPartyConfiguration(USER_ID, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be OK")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should be the expected configuration")
            .isEqualTo(config);

        verify(thirdPartyConfigurationService).findThirdPartyConfigurationByUserId(USER_ID);
    }

    @Test
    void testUpdateThirdPartyConfiguration() {
        ApiOauthConfigurationDto config = new ApiOauthConfigurationDto();

        ResponseEntity<String> response = controller.updateThirdPartyConfiguration(USER_ID, config, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be OK")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should contain the user ID")
            .contains(USER_ID.toString());

        verify(thirdPartyConfigurationService).updateThirdPartyConfigurationByUserId(USER_ID, config);
    }
}
