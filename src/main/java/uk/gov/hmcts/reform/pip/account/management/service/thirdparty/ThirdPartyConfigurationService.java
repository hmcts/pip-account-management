package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.ApiOauthConfigurationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;

import java.util.UUID;

@Service
@Slf4j
public class ThirdPartyConfigurationService {
    private final ApiOauthConfigurationRepository apiOauthConfigurationRepository;

    @Autowired
    public ThirdPartyConfigurationService(ApiOauthConfigurationRepository apiOauthConfigurationRepository) {
        this.apiOauthConfigurationRepository = apiOauthConfigurationRepository;
    }

    public ApiOauthConfiguration createThirdPartyConfiguration(ApiOauthConfiguration apiOauthConfiguration) {
        return apiOauthConfigurationRepository.save(apiOauthConfiguration);
    }

    public ApiOauthConfiguration findThirdPartyConfigurationByUserId(UUID userId) {
        return apiOauthConfigurationRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(
                String.format("OAuth configuration for Third-party user with ID %s could not be found", userId)
            ));
    }

    public void updateThirdPartyConfigurationByUserId(UUID userId, ApiOauthConfiguration apiOauthConfiguration) {
        ApiOauthConfiguration foundApiOauthConfiguration = findThirdPartyConfigurationByUserId(userId);
        updateExistingThirdPartyConfiguration(apiOauthConfiguration, foundApiOauthConfiguration);

    }

    public void deleteThirdPartyConfigurationByUserId(UUID userId) {
        apiOauthConfigurationRepository.deleteByUserId(userId);
    }

    private void updateExistingThirdPartyConfiguration(ApiOauthConfiguration suppliedApiOAuthConfiguration,
                                                       ApiOauthConfiguration existingApiOauthConfiguration) {
        suppliedApiOAuthConfiguration.setId(existingApiOauthConfiguration.getId());
        suppliedApiOAuthConfiguration.setCreatedDate(existingApiOauthConfiguration.getCreatedDate());
        suppliedApiOAuthConfiguration.setLastUpdatedDate(existingApiOauthConfiguration.getLastUpdatedDate());
        apiOauthConfigurationRepository.save(suppliedApiOAuthConfiguration);
    }
}
