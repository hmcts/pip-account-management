package uk.gov.hmcts.reform.pip.account.management.service.subscription;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.model.enums.UserActions;

import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.subscription.SearchType.LOCATION_ID;

@Slf4j
@Service
public class SubscriptionListTypeService {
    private final SubscriptionListTypeRepository subscriptionListTypeRepository;
    private final UserRepository userRepository;

    @Autowired
    public SubscriptionListTypeService(SubscriptionListTypeRepository subscriptionListTypeRepository,
                                       UserRepository userRepository) {
        this.subscriptionListTypeRepository = subscriptionListTypeRepository;
        this.userRepository = userRepository;
    }

    public void addListTypesForSubscription(SubscriptionListType subscriptionListType, String userId) {
        log.info(writeLog(userId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));

        if (userRepository.findByUserId(subscriptionListType.getUserId()).isEmpty()) {
            throw new UserNotFoundException("userId", subscriptionListType.getUserId().toString());
        }

        subscriptionListTypeRepository.deleteByUserId(subscriptionListType.getUserId());
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    public void configureListTypesForSubscription(SubscriptionListType subscriptionListType, String userId) {
        log.info(writeLog(userId, UserActions.CREATE_SUBSCRIPTION, LOCATION_ID.name()));

        if (userRepository.findByUserId(subscriptionListType.getUserId()).isEmpty()) {
            throw new UserNotFoundException("userId", subscriptionListType.getUserId().toString());
        }

        Optional<SubscriptionListType> existingSubscriptionListType = subscriptionListTypeRepository
            .findByUserId(subscriptionListType.getUserId());
        existingSubscriptionListType.ifPresent(listType -> subscriptionListType.setId(listType.getId()));
        subscriptionListTypeRepository.save(subscriptionListType);
    }

    public void deleteListTypesForSubscription(UUID userId) {
        Optional<SubscriptionListType> subscriptionListTypes =
            subscriptionListTypeRepository.findByUserId(userId);

        subscriptionListTypes.ifPresent(subscriptionListType -> subscriptionListTypeRepository
            .deleteByUserId(subscriptionListType.getUserId()));
    }
}
