package uk.gov.hmcts.reform.demo.validation.validator;

import uk.gov.hmcts.reform.demo.validation.annotations.ValidEmail;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * This class validates that the email is correct.
 *
 * <p>The email must contain an @ symbol</p>
 */
public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    /**
     * Logic to validate the email.
     *
     * @param email The email provided.
     * @param context The context for the validator.
     * @return Whether the email is valid.
     */
    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return email != null && email.contains("@");
    }
}
