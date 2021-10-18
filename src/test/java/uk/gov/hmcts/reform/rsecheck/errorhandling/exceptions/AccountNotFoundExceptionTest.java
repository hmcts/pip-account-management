package uk.gov.hmcts.reform.rsecheck.errorhandling.exceptions;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.demo.errorhandling.exceptions.AccountNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountNotFoundExceptionTest {

    @Test
    void testCreationOfAccountNotFoundException() {

        AccountNotFoundException subscriptionNotFoundException
            = new AccountNotFoundException("This is a test message");
        assertEquals("This is a test message", subscriptionNotFoundException.getMessage(),
                     "The message should match the message passed in");

    }

}
