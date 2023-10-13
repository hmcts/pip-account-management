package uk.gov.hmcts.reform.pip.account.management.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.ALL_NON_RESTRICTED_ADMIN_ROLES;
import static uk.gov.hmcts.reform.pip.model.account.Roles.ALL_NON_THIRD_PARTY_ROLES;

@Service("authorisationService")
@Slf4j
public class AuthorisationService {
    @Autowired
    UserRepository userRepository;

    public boolean userCanDeleteAccount(UUID userId, UUID adminUserId) {
        boolean isAuthorised = isAuthorisedRole(userId, adminUserId);

        if (!isAuthorised) {
            log.error(writeLog(
                String.format("User with ID %s is forbidden to remove user with ID %s", adminUserId, userId)
            ));
        }
        return isAuthorised;
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

    private boolean isAuthorisedRole(UUID userId, UUID adminUserId) {
        if (adminUserId == null) {
            return false;
        }

        Roles userRole = getUserRole(userId);
        Roles adminUserRole = getUserRole(adminUserId);

        if (adminUserRole == Roles.SYSTEM_ADMIN) {
            return ALL_NON_THIRD_PARTY_ROLES.contains(userRole);
        } else if (adminUserRole == Roles.INTERNAL_SUPER_ADMIN_LOCAL
            || adminUserRole == Roles.INTERNAL_SUPER_ADMIN_CTSC) {
            return ALL_NON_RESTRICTED_ADMIN_ROLES.contains(userRole);
        }
        return false;
    }

    private Roles getUserRole(UUID userId) {
        PiUser user = userRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(
                String.format("User with supplied user id: %s could not be found", userId)));
        return user.getRoles();
    }
}
