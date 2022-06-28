package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomExceptionTest {

    private static final String TEST_MESSAGE = "This is a test message";
    private static final String ASSERTION_MESSAGE = "The message should match the message passed in";

    @Test
    void testCreationOfNotFoundException() {
        NotFoundException notFoundException = new NotFoundException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, notFoundException.getMessage(), ASSERTION_MESSAGE);
    }

    @Test
    void testCreationOfUserNotFoundException() {
        UserNotFoundException userNotFoundException = new UserNotFoundException("test", "testId");
        assertEquals("No user found with the test: testId", userNotFoundException.getMessage(), ASSERTION_MESSAGE);
    }

    @Test
    void testCreationOfCsvParseException() {
        CsvParseException csvParseException = new CsvParseException(TEST_MESSAGE);
        assertEquals("Failed to parse CSV File due to: " + TEST_MESSAGE, csvParseException.getMessage(),
                     ASSERTION_MESSAGE);
    }
}
