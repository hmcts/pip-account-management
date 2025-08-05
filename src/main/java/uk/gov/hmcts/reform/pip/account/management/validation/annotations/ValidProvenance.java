package uk.gov.hmcts.reform.pip.account.management.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = uk.gov.hmcts.reform.pip.account.management.validation.validator.ProvenanceValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProvenance {
    String message() default "VERIFIED role is only allowed when provenance is PI_AAD";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
