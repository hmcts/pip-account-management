package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ConditionalEmailValidatorTest {

    private final PiUser piUser = new PiUser();

    ConditionalEmailValidator conditionalEmailValidator = new ConditionalEmailValidator();

    @BeforeEach
    void setup() {
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setEmail("email@email.com");
        piUser.setProvenanceUserId("123");
    }

    @Test
    void testUniqueUserReturnsTrue() {
        assertTrue(
            conditionalEmailValidator.isValid(piUser, null),
            "Email format probably isn't quite right."
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"test@email.com", ""})
    void testTechnicalUserReturnsTrueRegardless(String email) {
        piUser.setRoles(Roles.TECHNICAL);
        piUser.setEmail(email);
        assertThat(conditionalEmailValidator.isValid(piUser, null))
            .as("Tech users should be valid regardless of email status")
            .isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"d.f@x", "d@x.", "b@b@", "test", "河蟹"})
    void testNonTechnicalUserValidationBehavesProperlyForFails(String email) {
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setEmail(email);
        assertThat(conditionalEmailValidator.isValid(piUser, null))
            .as("All of the parameters should return invalid.")
            .isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"d.f@x.co", "d-f-x@x.ph", "wilfred_owen@x.su", "test.for+google@gmail.com"})
    void testNonTechnicalUserValidationBehavesProperlyForPasses(String email) {
        piUser.setRoles(Roles.VERIFIED);
        piUser.setEmail(email);
        assertThat(conditionalEmailValidator.isValid(piUser, null))
            .as("All of the parameters should return valid.")
            .isTrue();
    }
}
