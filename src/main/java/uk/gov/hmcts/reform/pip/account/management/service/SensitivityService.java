package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import static uk.gov.hmcts.reform.pip.model.account.Roles.VERIFIED;
import static uk.gov.hmcts.reform.pip.model.account.UserProvenances.THIRD_PARTY;

/**
 * This class handles the checking whether a user has permission to see a publication.
 */
@Service
public class SensitivityService {
    /**
     * Checks the sensitivity / list type and user, to determine if they have permission to see the publication.
     * @param user The user to check permissions for.
     * @param listType The list type of the publication.
     * @param sensitivity The sensitivity of the publication.
     * @return true if user has permission to see the publication, false if not.
     */
    public boolean checkAuthorisation(PiUser user, ListType listType, Sensitivity sensitivity) {
        return switch (sensitivity) {
            case PUBLIC -> true;
            case PRIVATE -> Roles.getAllVerifiedRoles().contains(user.getRoles());
            case CLASSIFIED -> VERIFIED.equals(user.getRoles())
                && user.getUserProvenance().equals(listType.getAllowedProvenance())
                || THIRD_PARTY.equals(user.getUserProvenance())
                && listType.getAllowedThirdPartyRoles().contains(user.getRoles());
            default -> false;
        };
    }
}
