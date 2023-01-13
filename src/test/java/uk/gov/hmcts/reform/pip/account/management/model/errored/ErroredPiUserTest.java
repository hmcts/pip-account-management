package uk.gov.hmcts.reform.pip.account.management.model.errored;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;

class ErroredPiUserTest {
    private static ErroredPiUser erroredPiUser;

    @BeforeAll
    public static void setup() {
        erroredPiUser = new ErroredPiUser();
    }

    @Test
    void testEqualWithNullObject() {
        Assertions.assertNotEquals(null, erroredPiUser);
    }

    @Test
    void testEqualWithObject() {
        Assertions.assertNotEquals(erroredPiUser, new Object());
    }

    @Test
    void testEqualWithDifferentClassObject() {
        Assertions.assertNotEquals(erroredPiUser, new AzureAccount());
    }

    @Test
    void testEqualWithSameClassObject() {
        Assertions.assertEquals(erroredPiUser, new ErroredPiUser());
    }

    @Test
    void testHashcode() {
        Assertions.assertEquals(0, erroredPiUser.hashCode());
    }
}
