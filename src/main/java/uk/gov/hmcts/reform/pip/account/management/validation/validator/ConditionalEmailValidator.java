package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.apache.commons.validator.routines.EmailValidator;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.PiEmailConditionalValidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


public class ConditionalEmailValidator implements ConstraintValidator<PiEmailConditionalValidation, PiUser> {

    @Override
    public void initialize(PiEmailConditionalValidation email) {
    }

    @Override
    public boolean isValid(PiUser user, ConstraintValidatorContext context) {
        String email = user.getEmail();
        if (email == null || email.length() == 0 || email.length() > 254) {
            return false;
        }
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }
}
