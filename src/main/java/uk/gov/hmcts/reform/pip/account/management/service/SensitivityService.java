package uk.gov.hmcts.reform.pip.account.management.service;

import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;

import static uk.gov.hmcts.reform.pip.account.management.model.Roles.ALL_VERIFIED_ROLES;

/**
 * This class handles the checking whether a user has permission to see a publication.
 */
@Component
@SuppressWarnings({"PMD.LawOfDemeter"})
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
            case PRIVATE -> ALL_VERIFIED_ROLES.contains(user.getRoles());
            case CLASSIFIED -> Roles.VERIFIED.equals(user.getRoles())
                && user.getUserProvenance().equals(listType.getAllowedProvenance())
                || UserProvenances.THIRD_PARTY.equals(user.getUserProvenance())
                && listType.getAllowedThirdPartyRoles().contains(user.getRoles());
            default -> false;
        };
    }
}
