package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@AllArgsConstructor
@Slf4j
public class AuditAuthorisationService {
    private final AuthorisationCommonService authorisationCommonService;

    public boolean userCanViewAuditLogs(UUID userId) {
        if (!(authorisationCommonService.hasOAuthAdminRole() && authorisationCommonService.isSystemAdmin(userId))) {
            log.error(writeLog(String.format("User with ID %s is not authorised to view audit logs", userId)));
            return false;
        }
        return true;
    }
}
