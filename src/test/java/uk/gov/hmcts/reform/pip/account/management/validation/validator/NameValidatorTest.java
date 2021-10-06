package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NameValidatorTest {

    private static final String TITLE = "Title";
    private static final String FIRST_NAME = "First Name";
    private static final String SURNAME = "Surname";

    @Test
    void testValidAllNames() {
        Subscriber subscriber = new Subscriber();
        subscriber.setTitle(TITLE);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setSurname(SURNAME);

        NameValidator nameValidator = new NameValidator();
        assertTrue(nameValidator.isValid(subscriber, null), "All names present is marked as valid");
    }

    @Test
    void testInvalidFirstnameSurname() {
        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setSurname(SURNAME);

        NameValidator nameValidator = new NameValidator();
        assertFalse(nameValidator.isValid(subscriber, null), "Firstname and Surname is marked as invalid");
    }

    @Test
    void testValidFirstname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setFirstName(FIRST_NAME);

        assertTrue(nameValidator.isValid(subscriber, null), "Only firstname is marked as valid");
    }

    @Test
    void testValidTitleSurname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setTitle(TITLE);
        subscriber.setSurname(SURNAME);

        assertTrue(nameValidator.isValid(subscriber, null), "Title and surname is marked as valid");
    }

    @Test
    void testValidNone() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();

        assertTrue(nameValidator.isValid(subscriber, null), "No name is marked as valid");
    }

    @Test
    void testInvalidTitle() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setTitle(TITLE);

        assertFalse(nameValidator.isValid(subscriber, null), "Only title is marked as invalid");
    }

    @Test
    void testInvalidSurname() {
        NameValidator nameValidator = new NameValidator();

        Subscriber subscriber = new Subscriber();
        subscriber.setSurname(SURNAME);

        assertFalse(nameValidator.isValid(subscriber, null), "Only surname is marked as invalid");
    }


}
