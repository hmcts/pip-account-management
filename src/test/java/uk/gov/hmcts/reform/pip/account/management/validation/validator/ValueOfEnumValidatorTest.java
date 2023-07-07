package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationDto;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ValueOfEnumValidatorTest {

    private Validator validator;
    private ValidatorFactory validatorFactory;
    private final MediaApplicationDto mediaApplicationDto = new MediaApplicationDto();

    @BeforeEach
    void setup() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();

        mediaApplicationDto.setFullName("Test user");
        mediaApplicationDto.setEmail("test@justice.gov.uk");
        mediaApplicationDto.setEmployer("MoJ");
    }

    @AfterEach
    void rundown() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @ValueSource(strings = {"PENDING", "APPROVED", "REJECTED"})
    void testValidValues(String statusValue) {
        mediaApplicationDto.setStatus(statusValue);
        Set violations = validator.validate(mediaApplicationDto);

        assertTrue(violations.isEmpty(), "A value that is valid was marked as invalid");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"TEST", "NOT", "VALID"})
    void testInvalidValues(String statusValue) {
        mediaApplicationDto.setStatus(statusValue);
        Set violations = validator.validate(mediaApplicationDto);

        assertTrue(!violations.isEmpty(), "A value that is invalid was marked as valid");
    }
}
