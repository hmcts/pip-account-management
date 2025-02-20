package uk.gov.hmcts.reform.pip.account.management.model.errored;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;

class ErroredAzureAccountTest {
    private static ErroredAzureAccount erroredAzureAccount;

    @BeforeAll
    public static void setup() {
        erroredAzureAccount = new ErroredAzureAccount();
    }

    @Test
    void testEqualWithNullObject() {
        Assertions.assertNotEquals(null, erroredAzureAccount, "Errored account is null");
    }

    @Test
    void testEqualWithObject() {
        Assertions.assertNotEquals(erroredAzureAccount, new Object(),
                                   "Errored account is equal to generic object");
    }

    @Test
    void testEqualWithDifferentClassObject() {
        Assertions.assertNotEquals(erroredAzureAccount, new PiUser(), "Errored account is equal to PIUser");
    }

    @Test
    void testEqualWithSameClassObject() {
        Assertions.assertEquals(erroredAzureAccount, new ErroredAzureAccount(),
                                "Errored account is not equal to expected errored account");
    }

    @Test
    void testHashcode() {
        Assertions.assertEquals(0, erroredAzureAccount.hashCode(),
                                "Errored account matches the correct hashcode");
    }
}
