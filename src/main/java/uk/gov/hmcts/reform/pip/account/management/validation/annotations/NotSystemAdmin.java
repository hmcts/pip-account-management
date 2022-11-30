package uk.gov.hmcts.reform.pip.account.management.validation.annotations;

import uk.gov.hmcts.reform.pip.account.management.validation.validator.NotSystemAdminValidator;
import uk.gov.hmcts.reform.pip.account.management.validation.validator.ProvenanceUserIdValidator;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is used to start the not system admin validator
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotSystemAdminValidator.class)
public @interface NotSystemAdmin {

    String message() default "System admins must be created via the /account/add/system-admin endpoint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}