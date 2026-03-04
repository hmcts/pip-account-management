package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.ApiOauthConfigurationRepository;
import uk.gov.hmcts.reform.pip.account.management.database.ApiUserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.helpers.ThirdPartyHelper;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfigurationDto;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.UUID;

@Service
@Slf4j
public class ThirdPartyConfigurationService {
    private final ApiOauthConfigurationRepository apiOauthConfigurationRepository;
    private final ApiUserRepository apiUserRepository;

    @Autowired
    public ThirdPartyConfigurationService(ApiOauthConfigurationRepository apiOauthConfigurationRepository,
                                          ApiUserRepository apiUserRepository) {
        this.apiOauthConfigurationRepository = apiOauthConfigurationRepository;
        this.apiUserRepository = apiUserRepository;
    }

    public ApiOauthConfiguration createThirdPartyConfiguration(ApiOauthConfigurationDto apiOauthConfigurationDto) {
        ApiOauthConfiguration apiOauthConfiguration = mapDtoToEntity(apiOauthConfigurationDto);
        return apiOauthConfigurationRepository.save(apiOauthConfiguration);
    }

    public ApiOauthConfiguration findThirdPartyConfigurationByUserId(UUID userId) {
        return apiOauthConfigurationRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(
                String.format("OAuth configuration for Third-party user with ID %s could not be found", userId)
            ));
    }

    public void updateThirdPartyConfigurationByUserId(UUID userId, ApiOauthConfigurationDto apiOauthConfigurationDto) {
        ApiOauthConfiguration suppliedApiOauthConfiguration = mapDtoToEntity(apiOauthConfigurationDto);
        ApiOauthConfiguration foundApiOauthConfiguration = findThirdPartyConfigurationByUserId(userId);
        updateExistingThirdPartyConfiguration(suppliedApiOauthConfiguration, foundApiOauthConfiguration);
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

    private ApiOauthConfiguration mapDtoToEntity(ApiOauthConfigurationDto apiOauthConfigurationDto) {
        ApiOauthConfiguration apiOauthConfiguration = new ApiOauthConfiguration();
        apiOauthConfiguration.setUserId(apiOauthConfigurationDto.getUserId());
        apiOauthConfiguration.setDestinationUrl(apiOauthConfigurationDto.getDestinationUrl());
        apiOauthConfiguration.setTokenUrl(apiOauthConfigurationDto.getTokenUrl());

        ApiUser apiUser = apiUserRepository.findByUserId(apiOauthConfigurationDto.getUserId())
            .orElseThrow(() -> new NotFoundException(
                String.format("Third-party user with ID %s could not be found", apiOauthConfigurationDto.getUserId())
            ));

        String keyPrefix = ThirdPartyHelper.createSecretKeyPrefix(apiUser.getName(), apiUser.getUserId().toString());
        apiOauthConfiguration.setClientIdKey(keyPrefix + "client-id");
        apiOauthConfiguration.setClientSecretKey(keyPrefix + "client-secret");
        apiOauthConfiguration.setScopeKey(keyPrefix + "scope");
        return apiOauthConfiguration;
    }
}
