package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidName;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import static io.micrometer.core.instrument.util.StringUtils.isEmpty;

/**
 * This class validates that the name combinations are correct.
 *
 * <p>Valid combinations are:
 *  1) Title, Firstname, Surname
 *  2) Firstname
 *  3) Title, Surname</p>
 *
 */
public class NameValidator implements ConstraintValidator<ValidName, Subscriber> {

    /**
     * Logic to validate the subscribers name.
     *
     * @param subscriber The subscriber to validate.
     * @param context The context for the validator.
     * @return Whether the subscribers name is valid.
     */
    @Override
    public boolean isValid(Subscriber subscriber, ConstraintValidatorContext context) {

        if (!isEmpty(subscriber.getTitle()) && !isEmpty(subscriber.getFirstName())
                                                     && !isEmpty(subscriber.getSurname())) {
            return true;
        }

        if (isEmpty(subscriber.getTitle()) && !isEmpty(subscriber.getFirstName())
                                                           && isEmpty(subscriber.getSurname())) {
            return true;
        }

        if (isEmpty(subscriber.getTitle()) && isEmpty(subscriber.getFirstName())
            && isEmpty(subscriber.getSurname())) {
            return true;
        }

        return !isEmpty(subscriber.getTitle()) && isEmpty(subscriber.getFirstName())
            && !isEmpty(subscriber.getSurname());
    }
}
