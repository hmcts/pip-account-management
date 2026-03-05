package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

@Service
@AllArgsConstructor
@Slf4j
public class ThirdPartyAuthorisationService {
    private final AuthorisationCommonService authorisationCommonService;

    public boolean userCanManageThirdParty(UUID requesterId) {
        if (authorisationCommonService.hasOAuthAdminRole()
            && authorisationCommonService.isSystemAdmin(requesterId)) {
            return true;
        }

        log.error(writeLog(
            String.format("User with ID %s is not authorised to manage third parties", requesterId)
        ));
        return false;
    }
}
