package uk.gov.hmcts.reform.pip.account.management.validation.validator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class NotSystemAdminValidatorTest {

    NotSystemAdminValidatorAzureAccount notSystemAdminValidatorAzureAccount = new NotSystemAdminValidatorAzureAccount();

    @Test
    void testValidIfNull() {
        AzureAccount azureAccount = new AzureAccount();

        assertTrue(notSystemAdminValidatorAzureAccount.isValid(azureAccount, null),
                   "Validator has returned false when role is null");
    }

    @Test
    void testNotValidIfSystemAdmin() {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setRole(Roles.SYSTEM_ADMIN);

        assertFalse(notSystemAdminValidatorAzureAccount.isValid(azureAccount, null),
                   "Validator has returned true when role is system admin");
    }

    @Test
    void testValidIfNotSystemAdmin() {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setRole(Roles.VERIFIED);

        assertTrue(notSystemAdminValidatorAzureAccount.isValid(azureAccount, null),
                   "Validator has returned false when role is not system admin");
    }
}
