package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.NotSystemAdmin;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static uk.gov.hmcts.reform.pip.account.management.model.Roles.SYSTEM_ADMIN;

/**
 * Validator class that checks if an azure account is a system admin.
 */
public class NotSystemAdminValidatorAzureAccount implements ConstraintValidator<NotSystemAdmin, AzureAccount> {

    @Override
    public boolean isValid(AzureAccount azureAccount, ConstraintValidatorContext constraintValidatorContext) {
        return !SYSTEM_ADMIN.equals(azureAccount.getRole());
    }
}
