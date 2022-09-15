package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AzureEmailValidatorTest {

    AzureEmailValidator azureEmailValidator = new AzureEmailValidator();

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"d.f@x", "d@x.", "b@b@", "test", "河蟹"})
    void testFails(String email) {
        assertThat(azureEmailValidator.isValid(email, null))
            .as("All of the emails should return invalid.")
            .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"d.f@x.co", "d-f-x@x.ph", "wilfred_owen@x.su", "test.for+google@gmail.com"})
    void testPasses(String email) {
        assertThat(azureEmailValidator.isValid(email, null))
            .as("All of the emails should return valid.")
            .isTrue();
    }
}
