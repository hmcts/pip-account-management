package uk.gov.hmcts.reform.pip.account.management.validation.annotations;

import uk.gov.hmcts.reform.pip.account.management.validation.validator.NameValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.validation.Constraint;
import javax.validation.Payload;

/**
 * Annotation that is used to start the name validator.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NameValidator.class)
public @interface ValidName {

    String message() default "Invalid name provided. You must either provide no name, " +
        "or any of the following variations "
        + "1) Title, Firstname and Surname 2) Firstname 3) Title and Surname";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
