package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.service.AccountService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private static final String EMAIL = "a@b.com";

    private static final String TEST_BODY = "This is a returned string";
    private static final String STATUS_CODE_MATCH = "Status code responses should match";

    private static final String BODY_MATCH = "Body should match";

    private static final String TEST_ID_STRING_1 = "0b8968b4-5c79-4e4e-8f66-f6a552d9fa67";
    private static final String TEST_ID_STRING_2 = "0b8968b4-5c79-4e4e-8f66-f6a552d9fa68";
    private static final String TEST_ID_STRING_3 = "0b8968b4-5c79-4e4e-8f66-f6a552d9fa69";
    private static final String TEST_EMAIL_1 = "test@user.com";
    private static final String TEST_EMAIL_2 = "dave@email.com";

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

        when(accountService.addUsers(users, "test")).thenReturn(usersMap);

        ResponseEntity<Map<CreationEnum, List<?>>> response = accountController.createUsers("test", users);

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
    void testGetUserEmailsByIds() {
        List<String> userIdsList = new ArrayList<>();
        userIdsList.add(TEST_ID_STRING_1);
        userIdsList.add(TEST_ID_STRING_2);

        Map<String, Optional<String>> userEmailMap = new ConcurrentHashMap<>();
        userEmailMap.put(TEST_ID_STRING_1, Optional.of(TEST_EMAIL_1));
        userEmailMap.put(TEST_ID_STRING_2, Optional.of(TEST_EMAIL_2));

        when(accountService.findUserEmailsByIds(userIdsList)).thenReturn(userEmailMap);

        ResponseEntity<Map<String, Optional<String>>> response = accountController.getUserEmailsByIds(userIdsList);

        assertEquals(userEmailMap, response.getBody(), "Should return correct id and email map");
        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testGetUserEmailsByIdsNoEmail() {
        List<String> userIdsList = new ArrayList<>();
        userIdsList.add(TEST_ID_STRING_1);
        userIdsList.add(TEST_ID_STRING_2);

        Map<String, Optional<String>> userEmailMap = new ConcurrentHashMap<>();
        userEmailMap.put(TEST_ID_STRING_1, Optional.empty());
        userEmailMap.put(TEST_ID_STRING_2, Optional.of(TEST_EMAIL_2));
        userEmailMap.put(TEST_ID_STRING_3, Optional.empty());

        when(accountService.findUserEmailsByIds(userIdsList)).thenReturn(userEmailMap);

        ResponseEntity<Map<String, Optional<String>>> response = accountController.getUserEmailsByIds(userIdsList);

        assertEquals(userEmailMap, response.getBody(), "Should return correct id and email map");
        assertEquals(HttpStatus.OK, response.getStatusCode(), STATUS_CODE_MATCH);
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
    void testUpdateAccountRoleByIdWithoutAdminUser() {
        UUID userId = UUID.randomUUID();

        when(accountService.updateAccountRole(null, userId, Roles.SYSTEM_ADMIN))
            .thenReturn(TEST_BODY);

        ResponseEntity<String> response =
            accountController.updateAccountRoleById(userId, Roles.SYSTEM_ADMIN, null);

        assertThat(response.getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as(BODY_MATCH)
            .isEqualTo(TEST_BODY);
    }

    @Test
    void testUpdateAccountRoleByIdWithAdminUser() {
        UUID userId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();

        when(accountService.updateAccountRole(adminId, userId, Roles.SYSTEM_ADMIN))
            .thenReturn(TEST_BODY);

        ResponseEntity<String> response = accountController.updateAccountRoleById(userId, Roles.SYSTEM_ADMIN, adminId);

        assertThat(response.getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as(BODY_MATCH)
            .isEqualTo(TEST_BODY);
    }
}
