package uk.gov.hmcts.reform.pip.account.management.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uk.gov.hmcts.reform.pip.account.management.validation.validator.AzureEmailValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = AzureEmailValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AzureEmail {
    String message() default "must be a well-formed email address";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
