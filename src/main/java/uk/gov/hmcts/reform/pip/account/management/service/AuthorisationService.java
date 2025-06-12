package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import static uk.gov.hmcts.reform.pip.model.account.Roles.ALL_NON_RESTRICTED_ADMIN_ROLES;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;

@Service("authorisationService")
@AllArgsConstructor()
@Slf4j
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class AuthorisationService {
    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final AccountService accountService;

    public boolean userCanCreateAccount(UUID adminUserId, List<PiUser> users) {
        if (!isAdmin()) {
            return false;
        }

        for (PiUser user : users) {
            // Restrict third party user creation to SYSTEM_ADMIN only
            if (Roles.getAllThirdPartyRoles().contains(user.getRoles())) {
                Roles adminUserRole = getUser(adminUserId).getRoles();
                if (!SYSTEM_ADMIN.equals(adminUserRole)) {
                    log.error(writeLog(
                        String.format("User with ID %s is forbidden to create third party user", adminUserId)
                    ));
                    return false;
                }
                return true;
            }
            // Restrict PI AAD Verified user creation to CTSC Admin only
            if (Roles.getAllVerifiedRoles().contains(user.getRoles()) && user.getUserProvenance() == PI_AAD) {
                Roles adminUserRole = getUser(adminUserId).getRoles();
                if (!INTERNAL_ADMIN_CTSC.equals(adminUserRole)) {
                    log.error(writeLog(
                        String.format("User with ID %s is forbidden to create this verified user", adminUserId)
                    ));
                    return false;
                }
            }
        }
        return true;
    }

    public boolean userCanDeleteAccount(UUID userId, UUID adminUserId) {
        if (!isAdmin() || !isSystemAdmin(adminUserId) || adminUserId.equals(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to delete this account", adminUserId)));
            return false;
        }
        return true;
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

        boolean isAuthorised = isAuthorisedRole(userId, adminUserId);

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
        boolean isSystemAdmin = adminUser.isPresent() && adminUser.get().getRoles().equals(SYSTEM_ADMIN);

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

    public boolean userCanViewAuditLogs(UUID userId) {
        if (!isAdmin() || !isSystemAdmin(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to view audit logs", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanViewAccounts(UUID userId) {
        if (!isAdmin() || !isSystemAdmin(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to view accounts", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanBulkCreateMediaAccounts(UUID userId) {
        if (!isAdmin() || !isSystemAdmin(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to create these accounts", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanViewMediaApplications(UUID userId) {
        if (!isAdmin() || !isAdminCtsc(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to view media applications", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanUpdateMediaApplications(UUID userId) {
        if (!isAdmin() || !isAdminCtsc(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to update media "
                                                 + "applications", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanCreateAzureAccount(UUID userId) {
        if (!isAdmin() || !isAdminCtsc(userId)) {
            log.error(writeLog(String.format("User with ID %s is not authorised to create azure accounts", userId)));
            return false;
        }
        return true;
    }

    private boolean isAuthorisedRole(UUID userId, UUID adminUserId) {
        PiUser user = getUser(userId);
        if (UserProvenances.SSO.equals(user.getUserProvenance())) {
            return true;
        }

        if (adminUserId == null) {
            return false;
        }
        PiUser adminUser = getUser(adminUserId);

        if (adminUser.getRoles() == SYSTEM_ADMIN) {
            return true;
        } else if (adminUser.getRoles() == Roles.INTERNAL_SUPER_ADMIN_LOCAL
            || adminUser.getRoles() == Roles.INTERNAL_SUPER_ADMIN_CTSC) {
            return ALL_NON_RESTRICTED_ADMIN_ROLES.contains(user.getRoles());
        }
        return false;
    }

    private PiUser getUser(UUID userId) {
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(
                String.format("User with supplied user id: %s could not be found", userId)));
    }

    private boolean isSystemAdmin(UUID userId) {
        PiUser user = accountService.getUserById(userId);
        return user != null && user.getRoles() == SYSTEM_ADMIN;
    }

    private boolean isAdminCtsc(UUID userId) {
        PiUser user = accountService.getUserById(userId);
        return user != null && user.getRoles() == INTERNAL_ADMIN_CTSC;
    }

    private boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return hasAuthority(authentication, "APPROLE_api.request.admin");
    }

    private boolean hasAuthority(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(granted -> granted.getAuthority().equals(role));
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

}
