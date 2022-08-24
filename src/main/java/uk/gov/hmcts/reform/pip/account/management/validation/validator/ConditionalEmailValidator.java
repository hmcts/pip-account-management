package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.PiEmailConditionalValidation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionalEmailValidator implements ConstraintValidator<PiEmailConditionalValidation, PiUser> {

    @Override
    public void initialize(PiEmailConditionalValidation email) {
    }

    @Override
    public boolean isValid(PiUser user, ConstraintValidatorContext context) {
        if (user.getRoles().equals(Roles.TECHNICAL)) return true;

        String email = user.getEmail();
        if (email == null || email.length() == 0) return false;
        Pattern pattern = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}"
                                              + "~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b"
                                              + "\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:"
                                              + "(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*"
                                              + "[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.)"
                                              + "{3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:"
                                              + "(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\"
                                              + "\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

        Matcher m = pattern.matcher(email);
        return m.matches();
    }
}
