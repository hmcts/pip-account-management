package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.ApiOauthConfigurationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyConfigurationServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    @Mock
    private ThirdPartySubscriptionNotificationService thirdPartySubscriptionNotificationService;

    @InjectMocks
    private ThirdPartyConfigurationService service;

    @Test
    void testCreateThirdPartyConfiguration() {
        ApiOauthConfiguration config = new ApiOauthConfiguration();
        when(apiOauthConfigurationRepository.save(config)).thenReturn(config);

        ApiOauthConfiguration result = service.createThirdPartyConfiguration(config);

        assertThat(result)
            .as("Should return the saved configuration")
            .isSameAs(config);
    }

    @Test
    void testFindThirdPartyConfigurationByUserIdWhenConfigExists() {
        ApiOauthConfiguration config = new ApiOauthConfiguration();
        when(apiOauthConfigurationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(config));

        ApiOauthConfiguration result = service.findThirdPartyConfigurationByUserId(USER_ID);

        assertThat(result)
            .as("Should return the found configuration")
            .isSameAs(config);
    }

    @Test
    void testFindThirdPartyConfigurationByUserIdWhenConfigDoesNotExist() {
        when(apiOauthConfigurationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findThirdPartyConfigurationByUserId(USER_ID))
            .as("Should throw NotFoundException if configuration not found")
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void testUpdateThirdPartyConfigurationByUserId() {
        ApiOauthConfiguration config = new ApiOauthConfiguration();
        when(apiOauthConfigurationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(config));

        service.updateThirdPartyConfigurationByUserId(USER_ID, config);

        verify(apiOauthConfigurationRepository).save(config);
    }

    @Test
    void testDeleteThirdPartyConfigurationByUserId() {
        service.deleteThirdPartyConfigurationByUserId(USER_ID);

        verify(apiOauthConfigurationRepository).deleteByUserId(USER_ID);
    }

    @Test
    void testValidateThirdPartyConfigurationSuccess() {
        ApiOauthConfiguration config = new ApiOauthConfiguration();
        when(apiOauthConfigurationRepository.findByUserId(USER_ID)).thenReturn(Optional.of(config));

        service.validateThirdPartyConfiguration(USER_ID);

        verify(thirdPartySubscriptionNotificationService).handleThirdPartyHealthCheck(config);
    }

    @Test
    void testValidateThirdPartyConfigurationNotFound() {
        when(apiOauthConfigurationRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateThirdPartyConfiguration(USER_ID))
            .as("Should throw NotFoundException if configuration not found")
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }
}
