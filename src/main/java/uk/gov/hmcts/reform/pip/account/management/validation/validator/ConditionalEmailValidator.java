package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.apache.commons.validator.routines.EmailValidator;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.PiEmailConditionalValidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


public class ConditionalEmailValidator implements ConstraintValidator<PiEmailConditionalValidation, PiUser> {

    @Override
    public void initialize(PiEmailConditionalValidation email) {
    }

    /**
     * This is a note that the below validator wipes incoming email addresses when they are added erroneously to
     * third party creation requests. This prevents us having access to any data we shouldn't have. The suppression
     * is necessary to ensure that we can validate against the entirety of the third party roles tersely.
     * @param user object to validate
     * @param context context in which the constraint is evaluated
     *
     * @return boolean for whether it's successfully validated or not
     */
    @Override
    @SuppressWarnings("PMD.LawOfDemeter")
    public boolean isValid(PiUser user, ConstraintValidatorContext context) {
        if (Roles.ALL_THIRD_PARTY_ROLES.contains(user.getRoles())) {
            user.setEmail(null);
            return true;
        }
        String email = user.getEmail();
        if (email == null || email.length() == 0 || email.length() > 254) {
            return false;
        }
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }
}