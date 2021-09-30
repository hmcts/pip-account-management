package uk.gov.hmcts.reform.rsecheck.validation.validator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.validation.validator.NameValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NameValidatorTest {

    @Test
    public void testValidAllNames() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setTitle("Title");
        subscriber.setFirstName("First Name");
        subscriber.setSurname("Surname");

        assertTrue(nameValidator.isValid(subscriber, null));
    }

    @Test
    public void testValidFirstname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName("First Name");

        assertTrue(nameValidator.isValid(subscriber, null));
    }

    @Test
    public void testValidTitleSurname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setTitle("Title");
        subscriber.setSurname("First Name");

        assertTrue(nameValidator.isValid(subscriber, null));
    }

    @Test
    public void testInvalidTitle() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setTitle("Title");

        assertFalse(nameValidator.isValid(subscriber, null));
    }

    @Test
    public void testInvalidSurname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setSurname("Surname");

        assertFalse(nameValidator.isValid(subscriber, null));
    }

    @Test
    public void testInvalidNone() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();

        assertFalse(nameValidator.isValid(subscriber, null));
    }


}
