package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_SUPER_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.PI_AAD;

@Service
@AllArgsConstructor
@Slf4j
public class AccountAuthorisationService {

    private final UserRepository userRepository;
    private final AccountService accountService;
    private final AuthorisationCommonService authorisationCommonService;

    private boolean isAdminCtsc(UUID userId) {
        PiUser user = accountService.getUserById(userId);
        return user != null && user.getRoles() == INTERNAL_ADMIN_CTSC;
    }

    public boolean userCanCreateAccount(UUID adminUserId, List<PiUser> users) {
        if (!authorisationCommonService.hasOAuthAdminRole()) {
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
            // Restrict PI AAD Verified user creation to CTSC Admins only
            if (Roles.getAllVerifiedRoles().contains(user.getRoles()) && user.getUserProvenance() == PI_AAD) {
                Roles adminUserRole = getUser(adminUserId).getRoles();
                if (!List.of(INTERNAL_SUPER_ADMIN_CTSC, INTERNAL_ADMIN_CTSC).contains(adminUserRole)) {
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
        if (userId == null || adminUserId == null) {
            return false;
        }

        //System admins can delete any account, admin users can only delete their own account (via SSO process)
        if (authorisationCommonService.hasOAuthAdminRole()
            && (authorisationCommonService.isSystemAdmin(adminUserId)
            || (authorisationCommonService.isUserAdmin(adminUserId) && adminUserId.equals(userId)))) {

            return true;
        }
        log.error(writeLog(String.format("User with ID %s is not authorised to delete this account", adminUserId)));
        return false;
    }

    public boolean userCanUpdateAccount(UUID userId, UUID adminUserId) {
        if (!authorisationCommonService.hasOAuthAdminRole()) {
            return false;
        }

        PiUser user = getUser(userId);
        if (!UserProvenances.SSO.equals(user.getUserProvenance()) || adminUserId != null) {
            log.error(writeLog(String.format("Only SSO users can be updated via the automated process",
                                             adminUserId, userId)));
            return false;
        }

        return true;
    }

    public boolean userCanCreateSystemAdmin(UUID userId) {
        if (!authorisationCommonService.hasOAuthAdminRole()) {
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

    public boolean userCanGetAccountByUserId(UUID requesterId, UUID userId) {
        if (authorisationCommonService.hasOAuthAdminRole()
                && (authorisationCommonService.isUserAdmin(requesterId)
                || authorisationCommonService.isUserVerified(requesterId, userId))) {
            return true;
        }
        log.error(writeLog(String.format("User with ID %s is not authorised to get account by user ID", requesterId)));
        return false;
    }

    public boolean userCanViewAccounts(UUID requesterId) {
        if (authorisationCommonService.hasOAuthAdminRole() && authorisationCommonService.isSystemAdmin(requesterId)) {
            return true;
        }
        log.error(writeLog(String.format("User with ID %s is not authorised to view accounts", requesterId)));
        return false;
    }

    public boolean userCanBulkCreateMediaAccounts(UUID userId) {
        if (!(authorisationCommonService.hasOAuthAdminRole() && authorisationCommonService.isSystemAdmin(userId))) {
            log.error(writeLog(String.format("User with ID %s is not authorised to create these accounts", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanCreateAzureAccount(UUID userId) {
        if (!(authorisationCommonService.hasOAuthAdminRole() && isAdminCtsc(userId))) {
            log.error(writeLog(String.format("User with ID %s is not authorised to create azure accounts", userId)));
            return false;
        }
        return true;
    }

    private PiUser getUser(UUID userId) {
        return userRepository.findByUserId(userId)
            .orElseThrow(() -> new NotFoundException(
                String.format("User with supplied user id: %s could not be found", userId)));
    }
}
