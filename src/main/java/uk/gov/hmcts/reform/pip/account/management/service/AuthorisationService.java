package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service("authorisationService")
@Slf4j
@SuppressWarnings("PMD.TooManyMethods")
public class AuthorisationService {
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AccountService accountService;

    @Autowired
    public AuthorisationService(UserRepository userRepository, SubscriptionRepository subscriptionRepository,
                                AccountService accountService) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.accountService = accountService;
    }

    public boolean userCanCreateAccount(UUID adminUserId, List<PiUser> users) {
        if (!isAdmin()) {
            return false;
        }

        for (PiUser user : users) {
            // Restrict third party user creation to SYSTEM_ADMIN only
            if (Roles.getAllThirdPartyRoles().contains(user.getRoles())) {
                Roles adminUserRole = getUser(adminUserId).getRoles();
                if (!Roles.SYSTEM_ADMIN.equals(adminUserRole)) {
                    log.error(writeLog(
                        String.format("User with ID %s is forbidden to create third party user", adminUserId)
                    ));
                    return false;
                }
            }
        }
        return true;
    }

    public boolean userCanDeleteAccount(UUID userId, UUID adminUserId) {
        if (!isAdmin()) {
            return false;
        }

        boolean isAuthorised = isAuthorisedRoleDelete(userId, adminUserId);

        if (!isAuthorised) {
            log.error(writeLog(
                String.format("User with ID %s is forbidden to remove user with ID %s", adminUserId, userId)
            ));
        }
        return isAuthorised;
    }

    public boolean userCanUpdateAccount(UUID userId, UUID adminUserId) {
        if (!isAdmin()) {
            return false;
        }

        if (adminUserId != null && adminUserId.equals(userId)) {
            log.error(writeLog(
                String.format("User with ID %s is forbidden to update their own account", userId)
            ));
            return false;
        }

        boolean isAuthorised = isAuthorisedRoleUpdate(userId, adminUserId);

        if (!isAuthorised) {
            log.error(writeLog(
                String.format("User with ID %s is forbidden to update user with ID %s", adminUserId, userId)
            ));
        }
        return isAuthorised;
    }

    public boolean userCanCreateSystemAdmin(UUID userId) {
        if (!isAdmin()) {
            return false;
        }

        Optional<PiUser> adminUser = userRepository.findByUserId(userId);
        boolean isSystemAdmin = adminUser.isPresent() && adminUser.get().getRoles().equals(Roles.SYSTEM_ADMIN);

        if (!isSystemAdmin) {
            log.error(writeLog(
                String.format("User with ID %s is forbidden to create a B2C system admin", userId)
            ));
        }
        return isSystemAdmin;
    }

    public boolean userCanDeleteSubscriptions(UUID userId, UUID... subscriptionIds) {
        return isAdmin() && (isSystemAdmin(userId) || Arrays.stream(subscriptionIds)
            .allMatch(id -> isSubscriptionUserMatch(id, userId)));

    }

    private boolean isAuthorisedRoleUpdate(UUID userId, UUID adminUserId) {
        PiUser user = getUser(userId);

        //Triggered by the user creation process for SSO
        if (UserProvenances.SSO.equals(user.getUserProvenance()) && adminUserId == null) {
            return true;
        }

        if (adminUserId == null) {
            return false;
        }

        PiUser adminUser = getUser(adminUserId);
        return adminUser.getRoles() == Roles.SYSTEM_ADMIN && Roles.getAllThirdPartyRoles().contains(user.getRoles());
    }

    private boolean isAuthorisedRoleDelete(UUID userId, UUID adminUserId) {
        PiUser user = getUser(userId);

        //Triggered by the user creation process for SSO
        if (UserProvenances.SSO.equals(user.getUserProvenance()) && adminUserId == null) {
            return true;
        }

        if (adminUserId == null) {
            return false;
        }

        PiUser adminUser = getUser(adminUserId);
        return adminUser.getRoles() == Roles.SYSTEM_ADMIN;
    }

    private PiUser getUser(UUID userId) {
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(
                String.format("User with supplied user id: %s could not be found", userId)));
    }

    private boolean isSystemAdmin(UUID userId) {
        PiUser user = accountService.getUserById(userId);
        return user != null && user.getRoles() == Roles.SYSTEM_ADMIN;
    }

    private boolean isSubscriptionUserMatch(UUID subscriptionId, UUID userId) {
        Optional<Subscription> subscription = subscriptionRepository.findById(subscriptionId);

        if (subscription.isPresent()) {
            if (userId.toString().equals(subscription.get().getUserId().toString())) {
                return true;
            }
            log.error(writeLog(
                String.format("User %s is forbidden to remove subscription with ID %s belongs to another user %s",
                              userId, subscriptionId, subscription.get().getUserId())
            ));
            return false;
        }
        // Return true if not subscription found. It will then go to the delete subscription method and return
        // 404 HTTP status rather than a 403 forbidden status.
        return true;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return hasAuthority(authentication, "APPROLE_api.request.admin");
    }

    private boolean hasAuthority(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(granted -> granted.getAuthority().equals(role));
    }
}
