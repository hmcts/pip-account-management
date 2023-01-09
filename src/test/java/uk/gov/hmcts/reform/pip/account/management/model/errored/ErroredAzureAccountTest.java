package uk.gov.hmcts.reform.pip.account.management.model.errored;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;

class ErroredAzureAccountTest {
    private static ErroredAzureAccount erroredAzureAccount;

    @BeforeAll
    public static void setup() {
        erroredAzureAccount = new ErroredAzureAccount();
    }

    @Test
    void testEqualWithNullObject() {
        Assertions.assertNotEquals(null, erroredAzureAccount);
    }

    @Test
    void testEqualWithObject() {
        Assertions.assertNotEquals(erroredAzureAccount, new Object());
    }

    @Test
    void testEqualWithDifferentClassObject() {
        Assertions.assertNotEquals(erroredAzureAccount, new PiUser());
    }

    @Test
    void testEqualWithSameClassObject() {
        Assertions.assertEquals(erroredAzureAccount, new ErroredAzureAccount());
    }

    @Test
    void testHashcode() {
        Assertions.assertEquals(0, erroredAzureAccount.hashCode());
    }
}
