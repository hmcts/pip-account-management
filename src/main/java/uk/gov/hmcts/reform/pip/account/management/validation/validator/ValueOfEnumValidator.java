package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValueOfEnum;

import java.util.List;
import java.util.stream.Stream;

/**
 * Validator class that checks if an input is in the supplied enum.
 */
public class ValueOfEnumValidator implements ConstraintValidator<ValueOfEnum, CharSequence> {
    private List<String> acceptedValues;

    @Override
    public void initialize(ValueOfEnum annotation) {
        acceptedValues = Stream.of(annotation.enumClass().getEnumConstants())
            .map(Enum::name)
            .toList();
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        return value == null || acceptedValues.contains(value.toString());
    }
}
