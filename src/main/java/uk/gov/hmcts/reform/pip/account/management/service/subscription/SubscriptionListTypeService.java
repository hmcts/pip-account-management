package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.util.Optional;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.LOCATION_ID;

@Slf4j
@Service
public class SubscriptionListTypeService {
    private final SubscriptionListTypeRepository subscriptionListTypeRepository;

    @Autowired
    public SubscriptionListTypeService(SubscriptionListTypeRepository subscriptionListTypeRepository) {
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
    }

    public void addListTypesForSubscription(SubscriptionListType subscriptionListType, String userId) {
        log.info(writeLog(userId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));
        subscriptionListTypeRepository.deleteByUserId(subscriptionListType.getUserId());
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    public void configureListTypesForSubscription(SubscriptionListType subscriptionListType, String userId) {
        log.info(writeLog(userId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));

        Optional<SubscriptionListType> existingSubscriptionListType = subscriptionListTypeRepository
            .findByUserId(subscriptionListType.getUserId());
        existingSubscriptionListType.ifPresent(listType -> subscriptionListType.setId(listType.getId()));
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    public void deleteListTypesForSubscription(String userId) {
        Optional<SubscriptionListType> subscriptionListTypes = subscriptionListTypeRepository.findByUserId(userId);

        subscriptionListTypes.ifPresent(subscriptionListType -> subscriptionListTypeRepository
            .deleteByUserId(subscriptionListType.getUserId()));
    }
}
