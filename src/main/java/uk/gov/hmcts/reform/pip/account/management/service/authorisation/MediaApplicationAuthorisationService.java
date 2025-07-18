package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;

import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;

@Service
@AllArgsConstructor
@Slf4j
public class MediaApplicationAuthorisationService {
    private final UserRepository userRepository;
    private final AccountService accountService;
    private final AuthorisationCommonService authorisationCommonService;

    private boolean isAdminCtsc(UUID userId) {
        PiUser user = accountService.getUserById(userId);
        return user != null && user.getRoles() == INTERNAL_ADMIN_CTSC;
    }


    public boolean userCanBulkCreateMediaAccounts(UUID userId) {
        if (!(authorisationCommonService.isAdmin() && authorisationCommonService.isSystemAdmin(userId))) {
            log.error(writeLog(String.format("User with ID %s is not authorised to create these accounts", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanViewMediaApplications(UUID userId) {
        if (!(authorisationCommonService.isAdmin() && isAdminCtsc(userId))) {
            log.error(writeLog(String.format("User with ID %s is not authorised to view media applications", userId)));
            return false;
        }
        return true;
    }

    public boolean userCanUpdateMediaApplications(UUID userId) {
        if (!(authorisationCommonService.isAdmin() && isAdminCtsc(userId))) {
            log.error(writeLog(String.format("User with ID %s is not authorised to update media "
                                                 + "applications", userId)));
            return false;
        }
        return true;
    }
}
