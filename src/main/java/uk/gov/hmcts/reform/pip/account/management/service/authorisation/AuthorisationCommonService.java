package uk.gov.hmcts.reform.pip.account.management.service.authorisation;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;

import java.util.UUID;

import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;

@Service
@AllArgsConstructor
@Slf4j
public class AuthorisationCommonService {

    private final AccountService accountService;

    public boolean hasOAuthAdminRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return hasAuthority(authentication, "APPROLE_api.request.admin");
    }

    public boolean hasAuthority(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
            .anyMatch(granted -> granted.getAuthority().equals(role));
    }

    public boolean isSystemAdmin(UUID userId) {
        if (userId == null) {
            return false;
        }

        PiUser user = accountService.getUserById(userId);
        return user != null && user.getRoles() == SYSTEM_ADMIN;
    }
}
