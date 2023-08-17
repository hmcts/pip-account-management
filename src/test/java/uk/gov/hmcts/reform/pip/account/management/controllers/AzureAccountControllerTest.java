package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.service.AzureAccountService;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AzureAccountControllerTest {
    private static final String EMAIL = "a@b.com";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    AzureAccountService azureAccountService;

    @InjectMocks
    AzureAccountController azureAccountController;

    @Test
    void createAzureAccount() {
        Map<CreationEnum, List<? extends AzureAccount>> accountsMap = new ConcurrentHashMap<>();
        accountsMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new AzureAccount()));

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);

        List<AzureAccount> azureAccounts = List.of(azureAccount);

        when(azureAccountService.addAzureAccounts(argThat(arg -> arg.equals(azureAccounts)),
                                             eq("b@c.com"), eq(false), eq(false))).thenReturn(accountsMap);

        ResponseEntity<Map<CreationEnum, List<? extends AzureAccount>>> response =
            azureAccountController.createAzureAccount("b@c.com", azureAccounts);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(accountsMap, response.getBody(), "Should return the expected azureAccounts map");
    }

    @Test
    void testGetSystemAdminInfo() {
        AzureAccount user = new AzureAccount();
        user.setDisplayName("DisplayName");
        user.setEmail(EMAIL);

        String issuerId = UUID.randomUUID().toString();

        when(azureAccountService.retrieveAzureAccount(issuerId)).thenReturn(user);

        ResponseEntity<AzureAccount> response = azureAccountController.getAzureAccountInfo(issuerId);

        assertEquals(user, response.getBody(), "Should return found user");
        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
    }
}
