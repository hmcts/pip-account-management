package uk.gov.hmcts.reform.demo.validation.annotations;

import uk.gov.hmcts.reform.demo.validation.validator.EmailValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Annotation that is used to start the email Validator.
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailValidator.class)
public @interface ValidEmail {

    String message() default "Invalid email provided. Email must contain an @ symbol";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
