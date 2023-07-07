package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.EmailValidator;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.PiEmailConditionalValidation;
import uk.gov.hmcts.reform.pip.model.account.Roles;


public class ConditionalEmailValidator implements ConstraintValidator<PiEmailConditionalValidation, PiUser> {
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
    public boolean isValid(PiUser user, ConstraintValidatorContext context) {
        if (Roles.getAllThirdPartyRoles().contains(user.getRoles())) {
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
