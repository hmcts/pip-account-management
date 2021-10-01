package uk.gov.hmcts.reform.rsecheck.validation.validator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.demo.validation.validator.EmailValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailValidatorTest {

    @Test
    void testValidEmail() {
        EmailValidator emailValidator = new EmailValidator();
        assertTrue(emailValidator.isValid("a@b.com", null), "Email with @ is marked as valid");
    }

    @Test
    void testNoEmail() {
        EmailValidator emailValidator = new EmailValidator();
        assertFalse(emailValidator.isValid(null, null), "Null email is marked as invalid");
    }

    @Test
    void testInvalidEmail() {
        EmailValidator emailValidator = new EmailValidator();
        assertFalse(emailValidator.isValid("ab.com", null), "Email without "
            + "@ is marked as invalid");
    }

}
