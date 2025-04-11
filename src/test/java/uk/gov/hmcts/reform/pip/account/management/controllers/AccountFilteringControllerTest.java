package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.AccountFilteringService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountFilteringControllerTest {
    private static final String EMAIL = "a@b.com";
    private static final String USER_ID = "1234";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    AccountFilteringService accountFilteringService;

    @InjectMocks
    AccountFilteringController accountFilteringController;

    @Test
    void testMiDataV2ReturnsOk() {
        AccountMiData accountMiData = new AccountMiData();
        accountMiData.setProvenanceUserId("1234");

        when(accountFilteringService.getAccountDataForMi()).thenReturn(List.of(accountMiData));
        ResponseEntity<List<AccountMiData>> listAccountMiData = accountFilteringController.getMiDataV2();
        assertEquals(HttpStatus.OK, listAccountMiData.getStatusCode(), STATUS_CODE_MATCH);
        assertTrue(listAccountMiData.getBody().contains(accountMiData), "Expected Account MI Data not found");
    }

    @Test
    void testRetrieveThirdPartyAccounts() {
        UUID uuid = UUID.randomUUID();
        PiUser piUser = new PiUser();
        piUser.setUserId(uuid);

        List<PiUser> users = List.of(piUser);
        when(accountFilteringService.findAllThirdPartyAccounts()).thenReturn(users);

        ResponseEntity<List<PiUser>> response = accountFilteringController.getAllThirdPartyAccounts(USER_ID);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status code does not match");
        assertEquals(users, response.getBody(), "Expected users do not match");
    }

    @Test
    void testGetAllAccountsExceptThirdParty() {
        assertThat(accountFilteringController.getAllAccountsExceptThirdParty(
            USER_ID, 0, 25, EMAIL, "1234",
            List.of(UserProvenances.PI_AAD), List.of(Roles.VERIFIED), USER_ID).getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetAdminUserByEmailAndProvenance() {
        PiUser piUser = new PiUser();
        piUser.setEmail(EMAIL);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        when(accountFilteringService.getAdminUserByEmailAndProvenance(EMAIL, UserProvenances.PI_AAD))
            .thenReturn(piUser);

        ResponseEntity<PiUser> response = accountFilteringController.getAdminUserByEmailAndProvenance(
            EMAIL, UserProvenances.PI_AAD);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status code does not match");
        assertEquals(piUser, response.getBody(), "Expected PI user does not match");
    }
}
