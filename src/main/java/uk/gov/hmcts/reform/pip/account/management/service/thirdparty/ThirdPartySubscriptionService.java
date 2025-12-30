package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.ApiSubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class ThirdPartySubscriptionService {
    private final ApiSubscriptionRepository apiSubscriptionRepository;

    @Autowired
    public ThirdPartySubscriptionService(ApiSubscriptionRepository apiSubscriptionRepository) {
        this.apiSubscriptionRepository = apiSubscriptionRepository;
    }

    public List<ApiSubscription> createThirdPartySubscriptions(List<ApiSubscription> apiSubscriptions) {
        return apiSubscriptionRepository.saveAll(apiSubscriptions);
    }

    public List<ApiSubscription> findThirdPartySubscriptionsByUserId(UUID userId) {
        List<ApiSubscription> apiSubscriptions = apiSubscriptionRepository.findAllByUserId(userId);

        if (apiSubscriptions.isEmpty()) {
            throw new NotFoundException(
                String.format("Subscriptions for third-party user with ID %s could not be found", userId)
            );
        }
        return apiSubscriptions;
    }

    public void updateThirdPartySubscriptionsByUserId(UUID userId, List<ApiSubscription> apiSubscriptions) {
        apiSubscriptionRepository.deleteAllByUserId(userId);
        apiSubscriptionRepository.saveAll(apiSubscriptions);

    }

    public void deleteThirdPartySubscriptionsByUserId(UUID userId) {
        apiSubscriptionRepository.deleteAllByUserId(userId);
    }
}
