package uk.gov.hmcts.reform.pip.account.management.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SystemAdminAccountTest {

    private static SystemAdminAccount systemAdminAccount;

    private static final String PROVENANCE_ID = "1234-1234";

    @BeforeAll
    public static void setup() {
        systemAdminAccount = new SystemAdminAccount();
        systemAdminAccount.setEmail("EMAIL");
        systemAdminAccount.setFirstName("FIRST_NAME");
        systemAdminAccount.setSurname("SURNAME");
    }


    @Test
    void testConvertToAzureAccount() {
        AzureAccount azureAccount = systemAdminAccount.convertToAzureAccount();
        assertEquals(systemAdminAccount.getEmail(), azureAccount.getEmail(),
                     "Azure account does not contain the email address");
        assertEquals(systemAdminAccount.getFirstName(), azureAccount.getFirstName(),
                     "Azure account does not contain the first name");
        assertEquals(systemAdminAccount.getSurname(), azureAccount.getSurname(),
                     "Azure account does not contain the surname");
        assertEquals(Roles.SYSTEM_ADMIN, azureAccount.getRole(),
                     "Azure account does not contain the correct role");
    }

    @Test
    void testConvertToPiUser() {
        PiUser piUser = systemAdminAccount.convertToPiUser(PROVENANCE_ID);
        assertEquals(systemAdminAccount.getEmail(), piUser.getEmail(),
                     "PI account does not contain the email address");
        assertEquals(UserProvenances.PI_AAD, piUser.getUserProvenance(),
                     "PI account does not contain the correct provenance");
        assertEquals(PROVENANCE_ID, piUser.getProvenanceUserId(),
                     "PI account does not contain the correct provenance ID");
        assertNotNull(piUser.getCreatedDate(),
                     "PI account does not have the created date");
        assertNotNull(piUser.getLastVerifiedDate(),
                      "PI account does not have the last verified date");
        assertNotNull(piUser.getLastSignedInDate(),
                      "PI account does not have the last signed in date");
        assertEquals(Roles.SYSTEM_ADMIN, piUser.getRoles(),
                     "PI account does not contain the correct role");
    }

}
