package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@AllArgsConstructor
@Slf4j
@SuppressWarnings("PMD.SimplifyBooleanReturns")
public class SubscriptionAuthorisationService {

    private final SubscriptionRepository subscriptionRepository;
    private final AccountService accountService;
    private final AuthorisationCommonService authorisationCommonService;

    private boolean isVerifiedUser(UUID userId) {
        PiUser user = accountService.getUserById(userId);
        if (user == null) {
            return false;
        }
        return Roles.getAllVerifiedRoles().contains(user.getRoles());
    }

    public boolean userCanAddSubscriptions(UUID userId, Subscription subscription) {
        UUID subscriptionId = subscription.getId();

        boolean isSystemAdminAddingThirdParty = authorisationCommonService.isAdmin()
            && authorisationCommonService.isSystemAdmin(userId)
            && isThirdPartySubscription(subscription);

        boolean isVerifiedMatchingUser = isVerifiedUser(userId)
            && isSubscriptionUserMatch(subscriptionId, userId);

        if (isSystemAdminAddingThirdParty || isVerifiedMatchingUser) {
            return true;
        }

        log.error(writeLog(String.format("User with ID %s is not authorised to add subscription with ID %s",
                                         userId, subscriptionId)));
        return false;
    }

    public boolean userCanDeleteSubscriptions(UUID requesterId, UUID... subscriptionIds) {
        if (authorisationCommonService.isAdmin() && authorisationCommonService.isSystemAdmin(requesterId)) {
            return true;
        }
        if (isVerifiedUser(requesterId) && Arrays.stream(subscriptionIds)
            .allMatch(id -> isSubscriptionUserMatch(id, requesterId))) {
            return true;
        }
        log.error(writeLog(String.format("User with ID %s is not authorised to remove these subscriptions",
                                         requesterId)));
        return false;
    }

    public boolean userCanBulkDeleteSubscriptions(UUID requesterId, UUID... subscriptionIds) {
        if (isVerifiedUser(requesterId) && Arrays.stream(subscriptionIds)
            .allMatch(id -> isSubscriptionUserMatch(id, requesterId))) {
            return true;
        }
        log.error(writeLog(String.format("User with ID %s is not authorised to remove these subscriptions",
                                         requesterId)));
        return false;
    }

    public boolean userCanViewSubscriptions(UUID requesterId, UUID userId) {
        if (authorisationCommonService.isAdmin() && authorisationCommonService.isSystemAdmin(requesterId)) {
            return true;
        }
        if (isVerifiedUser(requesterId) && requesterId.equals(userId)) {
            return true;
        }
        log.error(writeLog(String.format("User with ID %s is not authorised to view these subscriptions",
                                         requesterId)));
        return false;
    }

    public boolean userCanUpdateSubscriptions(UUID requesterId, UUID userId) {
        if (!isVerifiedUser(requesterId) || !requesterId.equals(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to update subscriptions for user with "
                                                 + "ID %s", requesterId, userId)));
            return false;
        }
        return true;
    }

    private boolean isThirdPartySubscription(Subscription subscription) {
        PiUser user = accountService.getUserById(subscription.getUserId());
        return Roles.getAllThirdPartyRoles().contains(user.getRoles());
    }

    private boolean isSubscriptionUserMatch(UUID subscriptionId, UUID userId) {
        Optional<Subscription> subscription = subscriptionRepository.findById(subscriptionId);

        if (subscription.isPresent()) {
            if (userId.toString().equals(subscription.get().getUserId().toString())) {
                return true;
            }
            log.error(writeLog(
                String.format("User %s is forbidden to alter subscription with ID %s belongs to another user %s",
                              userId, subscriptionId, subscription.get().getUserId())
            ));
            return false;
        }
        // Return true if not subscription found. It will then go to the delete subscription method and return
        // 404 HTTP status rather than a 403 forbidden status.
        return true;
    }
}
