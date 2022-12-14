package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.NotSystemAdmin;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * Validator class that checks if an azure account is a system admin.
 */
public class NotSystemAdminValidatorAzureAccount implements ConstraintValidator<NotSystemAdmin, AzureAccount> {

    @Override
    public boolean isValid(AzureAccount azureAccount, ConstraintValidatorContext constraintValidatorContext) {
        return azureAccount.getRole() == null || !azureAccount.getRole().equals(Roles.SYSTEM_ADMIN);
    }
}
