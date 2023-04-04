package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.MediaCsv;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountModelMapperServiceTest {

    private static final String EMAIL = "test@email.com";
    private static final String FIRST_NAME = "buzz";
    private static final String SURNAME = "lightyear";
    private static final String LIST_MATCH = "Returned lists should match";
    private static final String ID = "123";
    private static final String EMPTY = "";

    private final AccountModelMapperService accountModelMapperService = new AccountModelMapperService();

    @Test
    void testCreateAzureUsersFromCsv() {
        MediaCsv mediaCsv = new MediaCsv();
        mediaCsv.setEmail(EMAIL);
        mediaCsv.setFirstName(FIRST_NAME);
        mediaCsv.setSurname(SURNAME);

        AzureAccount expected = new AzureAccount();
        expected.setEmail(EMAIL);
        expected.setFirstName(FIRST_NAME);
        expected.setSurname(SURNAME);
        expected.setRole(Roles.VERIFIED);

        assertEquals(List.of(expected),
                     accountModelMapperService.createAzureUsersFromCsv(List.of(mediaCsv)),
                     LIST_MATCH);
    }

    @Test
    void testCreateAzureUsersFromCsvMultiple() {
        MediaCsv mediaCsv = new MediaCsv();
        mediaCsv.setEmail(EMAIL);
        mediaCsv.setFirstName(EMPTY);
        mediaCsv.setSurname(EMPTY);

        AzureAccount expected = new AzureAccount();
        expected.setEmail(EMAIL);
        expected.setFirstName(EMAIL);
        expected.setSurname(EMPTY);
        expected.setRole(Roles.VERIFIED);

        assertEquals(List.of(expected, expected),
                     accountModelMapperService.createAzureUsersFromCsv(List.of(mediaCsv, mediaCsv)),
                     LIST_MATCH);
    }

    @Test
    void testCreateAzureUsersFromCsvEmpty() {
        assertEquals(new ArrayList<>(), accountModelMapperService.createAzureUsersFromCsv(List.of()), LIST_MATCH);
    }

    @Test
    void testCreateAzureUserNoFirstName() {
        MediaCsv mediaCsv = new MediaCsv();
        mediaCsv.setEmail(EMAIL);
        mediaCsv.setFirstName(EMPTY);
        mediaCsv.setSurname(EMPTY);

        AzureAccount expected = new AzureAccount();
        expected.setEmail(EMAIL);
        expected.setFirstName(EMAIL);
        expected.setSurname(EMPTY);
        expected.setRole(Roles.VERIFIED);

        assertEquals(List.of(expected, expected),
                     accountModelMapperService.createAzureUsersFromCsv(List.of(mediaCsv, mediaCsv)),
                     LIST_MATCH);
    }

    @Test
    void testCreatePiUsersFromAzureAccounts() {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setSurname(SURNAME);
        azureAccount.setRole(Roles.VERIFIED);
        azureAccount.setAzureAccountId(ID);

        PiUser expected = new PiUser();
        expected.setRoles(Roles.VERIFIED);
        expected.setProvenanceUserId(ID);
        expected.setUserProvenance(UserProvenances.PI_AAD);
        expected.setEmail(EMAIL);

        assertEquals(List.of(expected),
                     accountModelMapperService.createPiUsersFromAzureAccounts(List.of(azureAccount)),
                     LIST_MATCH);
    }

    @Test
    void testCreatePiUsersFromAzureAccountsMultiple() {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setSurname(SURNAME);
        azureAccount.setRole(Roles.VERIFIED);
        azureAccount.setAzureAccountId(ID);

        PiUser expected = new PiUser();
        expected.setRoles(Roles.VERIFIED);
        expected.setProvenanceUserId(ID);
        expected.setUserProvenance(UserProvenances.PI_AAD);
        expected.setEmail(EMAIL);

        assertEquals(List.of(expected, expected),
                     accountModelMapperService.createPiUsersFromAzureAccounts(List.of(azureAccount, azureAccount)),
                     LIST_MATCH);
    }

    @Test
    void testCreatePiUsersFromAzureAccountsEmpty() {
        assertEquals(new ArrayList<>(), accountModelMapperService.createPiUsersFromAzureAccounts(List.of()),
                     LIST_MATCH);
    }

}
