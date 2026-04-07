package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.ApiSubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.model.publication.ListType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        List<ApiSubscription> foundApiSubscriptions = findThirdPartySubscriptionsByUserId(userId);
        updateExistingThirdPartySubscriptions(apiSubscriptions, foundApiSubscriptions);

    }

    public void deleteThirdPartySubscriptionsByUserId(UUID userId) {
        apiSubscriptionRepository.deleteAllByUserId(userId);
    }

    private void updateExistingThirdPartySubscriptions(List<ApiSubscription> suppliedApiSubscriptions,
                                                       List<ApiSubscription> existingApiSubscriptions) {
        Map<ListType, ApiSubscription> suppliedMap = suppliedApiSubscriptions.stream()
            .collect(Collectors.toMap(ApiSubscription::getListType, s -> s));
        Map<ListType, ApiSubscription> existingMap = existingApiSubscriptions.stream()
            .collect(Collectors.toMap(ApiSubscription::getListType, s -> s));

        // Update existing subscriptions that are present in supplied list
        for (ListType listType : existingMap.keySet()) {
            if (suppliedMap.containsKey(listType)) {
                ApiSubscription existingSubscription = existingMap.get(listType);
                ApiSubscription suppliedSubscription = suppliedMap.get(listType);
                existingSubscription.setSensitivity(suppliedSubscription.getSensitivity());
            }
        }
        apiSubscriptionRepository.saveAll(
            existingMap.values().stream()
                .filter(subscription -> suppliedMap.containsKey(subscription.getListType()))
                .collect(Collectors.toList())
        );

        // Remove existing subscriptions not present in supplied list
        apiSubscriptionRepository.deleteAll(
            existingMap.values().stream()
                .filter(subscription -> !suppliedMap.containsKey(subscription.getListType()))
                .collect(Collectors.toList())
        );

        // Add new subscriptions that are not in existing list
        apiSubscriptionRepository.saveAll(
            suppliedMap.values().stream()
                .filter(subscription -> !existingMap.containsKey(subscription.getListType()))
                .collect(Collectors.toList())
        );
    }
}
