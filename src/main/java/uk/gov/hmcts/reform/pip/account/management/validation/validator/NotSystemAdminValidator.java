package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.NotSystemAdmin;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator class that checks if a user is a system admin
 */
public class NotSystemAdminValidator implements ConstraintValidator<NotSystemAdmin, PiUser> {

    @Override
    public boolean isValid(PiUser piUser, ConstraintValidatorContext constraintValidatorContext) {
        return !piUser.getRoles().equals(Roles.SYSTEM_ADMIN);
    }
}
