package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidProvenance;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

public class ProvenanceValidator implements ConstraintValidator<ValidProvenance, PiUser> {

    @Override
    public boolean isValid(PiUser piUser, ConstraintValidatorContext context) {
        return checkRoles(piUser);
    }

    public static boolean checkRoles(PiUser piUser) {
        if (Roles.VERIFIED.equals(piUser.getRoles())) {
            return !UserProvenances.SSO.equals(piUser.getUserProvenance());
        } else if (Roles.getAllThirdPartyRoles().contains(piUser.getRoles())) {
            return UserProvenances.THIRD_PARTY.equals(piUser.getUserProvenance());
        }

        return UserProvenances.SSO.equals(piUser.getUserProvenance());
    }

}
