package uk.gov.hmcts.reform.pip.account.management.validation.annotations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import uk.gov.hmcts.reform.pip.account.management.validation.validator.NotSystemAdminValidatorAzureAccount;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that is used to start the not system admin validator.
 */
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {NotSystemAdminValidatorAzureAccount.class})
public @interface NotSystemAdmin {

    String message() default "System admins must be created via the /account/add/system-admin endpoint";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
