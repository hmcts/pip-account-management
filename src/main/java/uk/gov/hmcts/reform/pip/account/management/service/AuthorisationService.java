package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.ALL_NON_RESTRICTED_ADMIN_ROLES;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.Roles.VERIFIED;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;

@Service("authorisationService")
@Slf4j
public class AuthorisationService {
    private final UserRepository userRepository;

    @Autowired
    public AuthorisationService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean userCanCreateAccount(UUID adminUserId, List<PiUser> users) {
        Roles adminUserRole = getUser(adminUserId).getRoles();
        for (PiUser user : users) {
            // Restrict third party user creation to SYSTEM_ADMIN only
            if (Roles.getAllThirdPartyRoles().contains(user.getRoles())) {
                if (!Roles.SYSTEM_ADMIN.equals(adminUserRole)) {
                    log.error(writeLog(
                        String.format("User with ID %s is forbidden to create third party user", adminUserId)
                    ));
                    return false;
                }
            }
            // Restrict PI_AAD Verified user creation to INTERNAL_ADMIN_CTSC only
            if (user.getUserProvenance() == PI_AAD && user.getRoles() == VERIFIED) {
                if (!Roles.INTERNAL_ADMIN_CTSC.equals(adminUserRole)) {
                    log.error(writeLog(
                        String.format("User with ID %s is forbidden to create a verified user", adminUserId)
                    ));
                    return false;
                }
            }
        }
        return true;
    }

    public boolean userCanDeleteAccount(UUID userId, UUID requesterId) {
        boolean isisAuthorisedUser = userIsSystemAdmin(requesterId);
        if (!isisAuthorisedUser || requesterId.equals(userId) ) {
            log.error(writeLog(
                String.format("User with ID %s is forbidden to remove user with ID %s", requesterId, userId)
            ));
            return false;
        }
        return true;
    }

    public boolean userCanUpdateAccount(UUID userId, UUID adminUserId) {
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
        Optional<PiUser> adminUser = userRepository.findByUserId(userId);
        boolean isSystemAdmin = adminUser.isPresent() && adminUser.get().getRoles().equals(Roles.SYSTEM_ADMIN);

        if (!isSystemAdmin) {
            log.error(writeLog(
                String.format("User with ID %s is forbidden to create a B2C system admin", userId)
            ));
        }
        return isSystemAdmin;
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

        if (adminUser.getRoles() == Roles.SYSTEM_ADMIN) {
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

    public boolean userCanRequestAuditLogs(UUID requesterId) {
        boolean isAuthorisedUser = userIsSystemAdmin(requesterId);
        if (!isAuthorisedUser) {
            log.error(writeLog(
                String.format("User with ID %s is not authorised to view audit logs", requesterId)
            ));
        }
        return isAuthorisedUser;
    }

    public boolean userCanViewAccountDetails(UUID requesterId) {
        boolean isAuthorisedUser = userIsSystemAdmin(requesterId);
        if (!isAuthorisedUser) {
            log.error(writeLog(
                String.format("User with ID %s is not authorised to view account details", requesterId)
            ));
        }
        return isAuthorisedUser;
    }

    public boolean userCanBulkCreateMediaAccounts(UUID requesterId) {
        boolean isAuthorisedUser = userIsSystemAdmin(requesterId);
        if (!isAuthorisedUser) {
            log.error(writeLog(
                String.format("User with ID %s is not authorised to create media accounts", requesterId)
            ));
        }
        return isAuthorisedUser;
    }

    public boolean userCanCreateAzureAccount(UUID requesterId) {
        boolean isAuthorisedUser = userIsAdminCTSC(requesterId);
        if (!isAuthorisedUser) {
            log.error(writeLog(
                String.format("User with ID %s is not authorised to create accounts", requesterId)
            ));
        }
        return isAuthorisedUser;
    }

    public boolean userCanViewMediaApplications(UUID requesterId) {
        boolean isAuthorisedUser = userIsAdminCTSC(requesterId);
        if (!isAuthorisedUser) {
            log.error(writeLog(
                String.format("User with ID %s is not authorised to view media applications", requesterId)
            ));
        }
        return isAuthorisedUser;
    }

    public boolean userCanUpdateMediaApplications(UUID requesterId) {
        boolean isAuthorisedUser = userIsAdminCTSC(requesterId);
        if (!isAuthorisedUser) {
            log.error(writeLog(
                String.format("User with ID %s is not authorised to update media applications", requesterId)
            ));
        }
        return isAuthorisedUser;
    }

    public boolean userIsSystemAdmin(UUID requesterId) {
        return getUser(requesterId).getRoles().equals(SYSTEM_ADMIN);
    }

    public boolean userIsAdminCTSC(UUID requesterId) {
        return getUser(requesterId).getRoles().equals(INTERNAL_ADMIN_CTSC);
    }
}
