package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.service.CustomAccountRetrievalService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomAccountRetrievalControllerTest {
    private static final String EMAIL = "a@b.com";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    CustomAccountRetrievalService customAccountRetrievalService;

    @InjectMocks
    CustomAccountRetrievalController customAccountRetrievalController;

    @Test
    void testMiDataReturnsOk() {
        assertEquals(HttpStatus.OK,
                     customAccountRetrievalController.getMiData().getStatusCode(),
                     STATUS_CODE_MATCH);
    }

    @Test
    void testRetrieveThirdPartyAccounts() {
        UUID uuid = UUID.randomUUID();
        PiUser piUser = new PiUser();
        piUser.setUserId(uuid);

        List<PiUser> users = List.of(piUser);
        when(customAccountRetrievalService.findAllThirdPartyAccounts()).thenReturn(users);

        ResponseEntity<List<PiUser>> response = customAccountRetrievalController.getAllThirdPartyAccounts();

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status code does not match");
        assertEquals(users, response.getBody(), "Expected users do not match");
    }

    @Test
    void testGetAllAccountsExceptThirdParty() {
        assertThat(customAccountRetrievalController.getAllAccountsExceptThirdParty(
            0, 25, "test", "1234",
            List.of(UserProvenances.PI_AAD), List.of(Roles.VERIFIED), "1234").getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void testGetAdminUserByEmailAndProvenance() {
        PiUser piUser = new PiUser();
        piUser.setEmail(EMAIL);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        when(customAccountRetrievalService.getAdminUserByEmailAndProvenance(EMAIL, UserProvenances.PI_AAD))
            .thenReturn(piUser);

        ResponseEntity<PiUser> response = customAccountRetrievalController.getAdminUserByEmailAndProvenance(EMAIL,
                                                                                             UserProvenances.PI_AAD);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status code does not match");
        assertEquals(piUser, response.getBody(), "Expected PI user does not match");
    }
}
