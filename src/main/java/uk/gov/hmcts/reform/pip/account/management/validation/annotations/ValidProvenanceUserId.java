package uk.gov.hmcts.reform.pip.account.management.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uk.gov.hmcts.reform.pip.account.management.validation.validator.ProvenanceUserIdValidator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is used to start the provenance user id Validator.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProvenanceUserIdValidator.class)
public @interface ValidProvenanceUserId {

    String message() default "Invalid Provenance User Id provided, already exists.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
