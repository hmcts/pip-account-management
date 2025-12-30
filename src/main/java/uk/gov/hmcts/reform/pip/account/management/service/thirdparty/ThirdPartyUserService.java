package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.ApiUserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ThirdPartyUserService {
    private final ApiUserRepository apiUserRepository;
    private final ThirdPartySubscriptionService thirdPartySubscriptionService;
    private final ThirdPartyConfigurationService thirdPartyConfigurationService;

    @Autowired
    public ThirdPartyUserService(
        ApiUserRepository apiUserRepository,
        ThirdPartySubscriptionService thirdPartySubscriptionService,
        ThirdPartyConfigurationService thirdPartyConfigurationService
    ) {
        this.apiUserRepository = apiUserRepository;
        this.thirdPartySubscriptionService = thirdPartySubscriptionService;
        this.thirdPartyConfigurationService = thirdPartyConfigurationService;
    }

    public ApiUser createThirdPartyUser(ApiUser apiUser) {
        return apiUserRepository.save(apiUser);
    }

    public List<ApiUser> getAllThirdPartyUsers() {
        return apiUserRepository.findAll();
    }

    public ApiUser findThirdPartyUser(UUID userId) {
        return apiUserRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(
                String.format("Third-party user with ID %s could not be found", userId)
        ));
    }

    public void deleteThirdPartyUser(UUID userId) {
        if (apiUserRepository.findByUserId(userId).isEmpty()) {
            throw new NotFoundException(String.format("Third-party user with ID %s could not be found", userId));
        }
        thirdPartySubscriptionService.deleteThirdPartySubscriptionsByUserId(userId);
        thirdPartyConfigurationService.deleteThirdPartyConfigurationByUserId(userId);
        apiUserRepository.deleteByUserId(userId);
    }
}
