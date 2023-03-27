package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSystemAdminAccount;

import java.util.List;

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
    void testCreationOfUserWithProvenanceNotFoundException() {
        UserWithProvenanceNotFoundException notFoundException = new UserWithProvenanceNotFoundException("testId");
        assertEquals("No user found with provenance user ID: testId", notFoundException.getMessage(),
                     ASSERTION_MESSAGE);
    }

    @Test
    void testCreationOfCsvParseException() {
        CsvParseException csvParseException = new CsvParseException(TEST_MESSAGE);
        assertEquals("Failed to parse CSV File due to: " + TEST_MESSAGE, csvParseException.getMessage(),
                     ASSERTION_MESSAGE);
    }

    @Test
    void testCreationOfForbiddenRoleUpdateException() {
        ForbiddenRoleUpdateException forbiddenRoleUpdateException = new ForbiddenRoleUpdateException(TEST_MESSAGE);
        assertEquals(TEST_MESSAGE, forbiddenRoleUpdateException.getMessage(), ASSERTION_MESSAGE);
    }

    @Test
    void testCreationOfSystemAdminException() {
        ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount();
        erroredSystemAdminAccount.setFirstName("Test");
        erroredSystemAdminAccount.setErrorMessages(List.of("Error message A"));

        SystemAdminAccountException systemAdminAccountException =
            new SystemAdminAccountException(erroredSystemAdminAccount);

        assertEquals(erroredSystemAdminAccount, systemAdminAccountException.getErroredSystemAdminAccount(),
                     "The system admin account should match the system admin passed in");
    }

}
