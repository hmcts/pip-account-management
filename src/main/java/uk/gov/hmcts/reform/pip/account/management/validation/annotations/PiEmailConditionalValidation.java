package uk.gov.hmcts.reform.pip.account.management.validation.annotations;

import uk.gov.hmcts.reform.pip.account.management.validation.validator.ConditionalEmailValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;


@Documented
@Constraint(validatedBy = ConditionalEmailValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface PiEmailConditionalValidation {
    String message() default "{Email}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
