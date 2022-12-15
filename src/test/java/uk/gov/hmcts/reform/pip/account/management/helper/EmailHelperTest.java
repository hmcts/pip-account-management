package uk.gov.hmcts.reform.pip.account.management.helper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.helpers.EmailHelper;

class EmailHelperTest {

    @Test
    void testMaskEmail() {
        Assertions.assertEquals(
            EmailHelper.maskEmail("testUser@email.com"), "t*******@email.com", "Email was not masked correctly");
    }

    @Test
    void testMaskEmailNotValidEmail() {
        Assertions.assertEquals(EmailHelper.maskEmail("abcde"), "a****",
                                "Email was not masked correctly");
    }

    @Test
    void testMaskEmailEmptyString() {
        Assertions.assertEquals(
            EmailHelper.maskEmail(""), "", "Email was not masked correctly");
    }

}
