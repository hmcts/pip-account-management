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
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String EMAIL = "a@b.com";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    void createAzureAccount() {
        Map<CreationEnum, List<? extends AzureAccount>> accountsMap = new ConcurrentHashMap<>();
        accountsMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new AzureAccount()));

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail("a@b.com");
        List<AzureAccount> azureAccounts = List.of(azureAccount);

        when(accountService.addAzureAccounts(argThat(arg -> arg.equals(azureAccounts)),
                                             eq("b@c.com"))).thenReturn(accountsMap);

        ResponseEntity<Map<CreationEnum, List<? extends AzureAccount>>> response =
            accountController.createAzureAccount("b@c.com", azureAccounts);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(accountsMap, response.getBody(), "Should return the expected subscribers map");
    }

    @Test
    void testCreateUser() {
        Map<CreationEnum, List<?>> usersMap = new ConcurrentHashMap<>();
        usersMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new PiUser()));

        PiUser user = new PiUser();
        user.setEmail(EMAIL);

        List<PiUser> users = List.of(user);

        when(accountService.addUsers(users, "test")).thenReturn(usersMap);

        ResponseEntity<Map<CreationEnum, List<?>>> response = accountController.createUsers("test", users);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                     STATUS_CODE_MATCH);
        assertEquals(usersMap, response.getBody(), "Should return the expected user map");
    }

    @Test
    void testGetUserByProvenanceId() {
        PiUser user = new PiUser();
        user.setProvenanceUserId("123");
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setEmail(EMAIL);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        when(accountService.findUserByProvenanceId(user.getUserProvenance(), user.getProvenanceUserId()))
            .thenReturn(user);

        assertEquals(user,
                     accountController.getUserByProvenanceId(user.getUserProvenance(), user.getProvenanceUserId())
                         .getBody(), "Should return found user");

        assertEquals(HttpStatus.OK, accountController.getUserByProvenanceId(user.getUserProvenance(),
                                                                            user.getProvenanceUserId()).getStatusCode(),
                     STATUS_CODE_MATCH);
    }

}
