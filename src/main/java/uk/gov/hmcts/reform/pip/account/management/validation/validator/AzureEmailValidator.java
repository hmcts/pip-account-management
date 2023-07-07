package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.validator.routines.EmailValidator;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.AzureEmail;

public class AzureEmailValidator implements ConstraintValidator<AzureEmail, String> {

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        EmailValidator validator = EmailValidator.getInstance();
        return validator.isValid(email);
    }
}
