package uk.gov.hmcts.reform.rsecheck.validation.validator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.demo.model.Subscriber;
import uk.gov.hmcts.reform.demo.validation.validator.NameValidator;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameValidatorTest {

    @Test
    void testValidAllNames() {
        Subscriber subscriber = new Subscriber();
        subscriber.setTitle("Title");
        subscriber.setFirstName("First Name");
        subscriber.setSurname("Surname");

        NameValidator nameValidator = new NameValidator();
        assertTrue(nameValidator.isValid(subscriber, null), "All names present is marked as valid");
    }

    @Test
    void testValidFirstname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName("First Name");

        assertTrue(nameValidator.isValid(subscriber, null), "Only firstname is marked as valid");
    }

    @Test
    void testValidTitleSurname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setTitle("Title");
        subscriber.setSurname("Surname");

        assertTrue(nameValidator.isValid(subscriber, null), "Title and surname is marked as valid");
    }

    @Test
    void testInvalidTitle() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setTitle("Title");

        assertFalse(nameValidator.isValid(subscriber, null), "Only title is marked as invalid");
    }

    @Test
    void testInvalidSurname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setSurname("Surname");

        assertFalse(nameValidator.isValid(subscriber, null), "Only surname is marked as invalid");
    }

    @Test
    void testInvalidNone() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();

        assertFalse(nameValidator.isValid(subscriber, null), "No name is marked as invalid");
    }

}
