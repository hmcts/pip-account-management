package uk.gov.hmcts.reform.demo.validation.validator;

import io.micrometer.core.instrument.util.StringUtils;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.validation.annotations.ValidName;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


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

        if (!StringUtils.isEmpty(subscriber.getTitle()) && !StringUtils.isEmpty(subscriber.getFirstName())
                                                     && !StringUtils.isEmpty(subscriber.getSurname())) {
            return true;
        }

        if (StringUtils.isEmpty(subscriber.getTitle()) && !StringUtils.isEmpty(subscriber.getFirstName())
                                                           && StringUtils.isEmpty(subscriber.getSurname())) {
            return true;
        }

        return !StringUtils.isEmpty(subscriber.getTitle()) && StringUtils.isEmpty(subscriber.getFirstName())
            && !StringUtils.isEmpty(subscriber.getSurname());
    }
}
