package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValueOfEnumValidatorTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;
    private final MediaApplication mediaApplication = new MediaApplication();

    @BeforeEach
    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        mediaApplication.setFullName("Test user");
        mediaApplication.setEmail("test@justice.gov.uk");
        mediaApplication.setEmployer("MoJ");
    }

    @AfterEach
    void rundown() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @NullSource
    void testInvalidValues(MediaApplicationStatus statusValue) {
        mediaApplication.setStatus(statusValue);
        Set violations = validator.validate(mediaApplication);

        assertTrue(!violations.isEmpty(), "A value that is invalid was marked as valid");
    }
}
