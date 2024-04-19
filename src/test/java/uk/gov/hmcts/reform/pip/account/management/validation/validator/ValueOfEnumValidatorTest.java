package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValueOfEnumValidatorTest {

    @Test
    void testNullStatusValue() {
        ValidatorFactory validatorFactory = Validation.buildDefaultValidatorFactory();
        Validator validator = validatorFactory.getValidator();
        MediaApplication mediaApplication = new MediaApplication();

        mediaApplication.setFullName("Test user");
        mediaApplication.setEmail("test@justice.gov.uk");
        mediaApplication.setEmployer("MoJ");

        mediaApplication.setStatus(null);
        Set violations = validator.validate(mediaApplication);

        assertTrue(!violations.isEmpty(), "A value that is invalid was marked as valid");
    }
}
