package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.account.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String EMAIL = "a@b.com";
    private static final String ISSUER_ID = "123";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";
    private static final String TEST_ID_STRING_1 = "0b8968b4-5c79-4e4e-8f66-f6a552d9fa67";

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AccountController accountController;

    @Test
    void testCreateUser() {
        Map<CreationEnum, List<?>> usersMap = new ConcurrentHashMap<>();
        usersMap.put(CreationEnum.CREATED_ACCOUNTS, List.of(new PiUser()));

        PiUser user = new PiUser();
        user.setEmail(EMAIL);

        List<PiUser> users = List.of(user);

        when(accountService.addUsers(users, ISSUER_ID)).thenReturn(usersMap);

        ResponseEntity<Map<CreationEnum, List<?>>> response = accountController.createUsers(ISSUER_ID, users);

        assertEquals(HttpStatus.CREATED, response.getStatusCode(),
                     STATUS_CODE_MATCH);

        assertEquals(usersMap, response.getBody(), "Should return the expected user map");
    }

    @Test
    void testIsUserAuthorised() {
        when(accountService.isUserAuthorisedForPublication(any(), any(), any())).thenReturn(true);
        assertEquals(
            HttpStatus.OK,
            accountController.checkUserAuthorised(UUID.randomUUID(), ListType.MAGISTRATES_PUBLIC_LIST,
                                                  Sensitivity.PUBLIC).getStatusCode(),
            STATUS_CODE_MATCH
        );
        assertEquals(
            true,
            accountController.checkUserAuthorised(UUID.randomUUID(), ListType.MAGISTRATES_PUBLIC_LIST,
                                                  Sensitivity.PUBLIC).getBody(),
            "Should return boolean value"
        );
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

    @Test
    void testUpdateAccount() {
        Map<String, String> updateParameters = Map.of(
            "key1", "value1",
            "key2", "value2"
        );
        String expectedString = "Account with provenance PI_AAD and provenance id 0b8968b4-5c79-4e4e-8f66-f6a552d9fa67 "
            + "has been updated";
        when(accountService.updateAccount(UserProvenances.PI_AAD, TEST_ID_STRING_1, updateParameters))
            .thenReturn(expectedString);

        ResponseEntity<String> response = accountController.updateAccount(UserProvenances.PI_AAD,
                                                                          TEST_ID_STRING_1, updateParameters);

        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
        assertEquals(expectedString, response.getBody(), "Body does not match expected");
    }

    @Test
    void testRetrieveUserById() {
        UUID uuid = UUID.randomUUID();
        PiUser piUser = new PiUser();
        piUser.setUserId(uuid);

        when(accountService.getUserById(uuid)).thenReturn(piUser);

        ResponseEntity<PiUser> response = accountController.getUserById(uuid);

        assertEquals(HttpStatus.OK, response.getStatusCode(), "Expected status code does not match");

        assertEquals(piUser, response.getBody(), "Expected PI user does not match");
    }

    @Test
    void testDeleteAccount() {
        assertThat(accountController.deleteAccount(UUID.randomUUID()).getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.OK);
    }

    @Test
    void testDeleteV2Account() {
        assertThat(accountController.deleteAccountV2(UUID.randomUUID(), UUID.randomUUID()).getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.OK);
    }
}
