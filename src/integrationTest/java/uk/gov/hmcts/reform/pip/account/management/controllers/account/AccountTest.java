package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert", "PMD.SignatureDeclareThrowsException"})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-admin-users.sql")
class AccountTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    GraphServiceClient graphClient;

    @Mock
    private UsersRequestBuilder usersRequestBuilder;

    @Mock
    private UserItemRequestBuilder userItemRequestBuilder;

    private static final String ROOT_URL = "/account";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String CREATE_MEDIA_USER_URL = "/application";
    private static final String GET_PROVENANCE_USER_URL = ROOT_URL + "/provenance/";
    private static final String UPDATE_ACCOUNT_URL = ROOT_URL + "/provenance/";
    private static final String CREATE_SYSTEM_ADMIN_URL = ROOT_URL + "/system-admin";

    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String SYSTEM_ADMIN_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba4";

    private static final String INVALID_EMAIL = "ab";
    private static final String SURNAME = "Surname";
    private static final String FIRST_NAME = "firstname";
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final String SUPER_ADMIN_ISSUER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba3";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String GIVEN_NAME = "Given Name";
    private static final String ID = "1234";
    private static final String ADDITIONAL_ID = "4321";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String ERROR_RESPONSE_USER_PROVENANCE = "No user found with provenance user ID: 1234";
    private static final String USER_SHOULD_MATCH = "Users should match";
    private static final String DELETE_PATH_V2 = "/v2/";
    private static final String UPDATE_PATH = "/update/";
    private static final String REPLACE_STRING = "%s/%s/%s";
    private static final String DELETE_USER_FAILURE = "Failed to delete user account";
    private static final String DELETE_USER_SUCCESS = "User deleted";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PiUser verifiedUser;
    private PiUser superAdminUser;

    private PiUser createUser(boolean valid, UserProvenances userProvenances, Roles role) {
        PiUser user = new PiUser();
        user.setEmail(valid ? EMAIL : INVALID_EMAIL);
        user.setProvenanceUserId(UUID.randomUUID().toString());
        user.setUserProvenance(userProvenances);
        user.setRoles(role);
        user.setForenames(GIVEN_NAME);
        user.setSurname(SURNAME);

        return user;
    }

    private PiUser createThirdPartyUser() {
        PiUser user = new PiUser();
        user.setForenames(GIVEN_NAME);
        user.setSurname(SURNAME);
        user.setUserProvenance(UserProvenances.THIRD_PARTY);
        user.setProvenanceUserId(UUID.randomUUID().toString());
        user.setEmail("");
        user.setRoles(Roles.GENERAL_THIRD_PARTY);

        return user;
    }

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void setup() {
        verifiedUser = createVerifiedUser(true);
        superAdminUser = createAdminUser(true, Roles.INTERNAL_SUPER_ADMIN_CTSC);

        User userToReturn = new User();
        userToReturn.setId(ID);
        userToReturn.setGivenName(GIVEN_NAME);

        User additionalUser = new User();
        additionalUser.setId(ADDITIONAL_ID);
        additionalUser.setGivenName(GIVEN_NAME);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn, additionalUser);
    }

    @Nested
    class CreateUserTests {

        @Test
        void testCreateSingleUser() throws Exception {
            createAndAssertVerifiedTestUser(1, verifiedUser);
        }

        @Test
        void testCreateSsoUser() throws Exception {
            createAndAssertTestUser(1, superAdminUser);
        }

        @Test
        void testCreateThirdPartyUser() throws Exception {
            PiUser thirdPartyUser = createThirdPartyUser();
            Map<CreationEnum, List<Object>> mappedResponse = createTestThirdPartyUser(thirdPartyUser);
            assertEquals(1, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                         "1 User should be created"
            );
        }

        @Test
        void testCreatePiAadUserWithoutRequestorIdFails() throws Exception {
            PiUser verifiedUser = new PiUser();
            verifiedUser.setEmail("a@test.com");
            verifiedUser.setProvenanceUserId(UUID.randomUUID().toString());
            verifiedUser.setUserProvenance(PROVENANCE);
            verifiedUser.setRoles(Roles.VERIFIED);

            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(verifiedUser)))
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isNotFound()).andReturn();
        }

        @Test
        void testCreateNonPiAadUserWithoutRequestorId() throws Exception {
            PiUser validUser = new PiUser();
            validUser.setEmail("sso-test-user-cath@justice.gov.uk");
            validUser.setProvenanceUserId(UUID.randomUUID().toString());
            validUser.setUserProvenance(UserProvenances.SSO);
            validUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);

            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        }

        @Test
        void testCreateMultipleSuccessUsers() throws Exception {
            User userToReturn = new User();
            userToReturn.setId(ID);
            userToReturn.setGivenName(GIVEN_NAME);

            when(graphClient.users()).thenReturn(usersRequestBuilder);
            when(usersRequestBuilder.byUserId(any())).thenReturn(userItemRequestBuilder);
            when(userItemRequestBuilder.get()).thenReturn(userToReturn);

            MockHttpServletRequestBuilder mockHttpServletRequestMediaUserBuilder = MockMvcRequestBuilders
                .get(CREATE_MEDIA_USER_URL)
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestMediaUserBuilder).andExpect(status().isOk()).andReturn();

            createAndAssertVerifiedTestUser(2, createVerifiedUser(true), createVerifiedUser(true));
        }

        @Test
        void testCreateMultipleSuccessUsersWithDifferentEmails() throws Exception {
            User userToReturn = new User();
            userToReturn.setId(ID);
            userToReturn.setGivenName(GIVEN_NAME);

            when(graphClient.users()).thenReturn(usersRequestBuilder);
            when(usersRequestBuilder.byUserId(any())).thenReturn(userItemRequestBuilder);
            when(userItemRequestBuilder.get()).thenReturn(userToReturn);

            MockHttpServletRequestBuilder mockHttpServletRequestMediaUserBuilder = MockMvcRequestBuilders
                .get(CREATE_MEDIA_USER_URL)
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestMediaUserBuilder).andExpect(status().isOk()).andReturn();

            PiUser validUser2 = new PiUser();
            validUser2.setEmail("a@test.com");
            validUser2.setProvenanceUserId(UUID.randomUUID().toString());
            validUser2.setUserProvenance(PROVENANCE);
            validUser2.setRoles(Roles.VERIFIED);

            createAndAssertVerifiedTestUser(2, createVerifiedUser(true), validUser2);
        }

        @Test
        void testCreateSingleErroredUser() throws Exception {
            Map<CreationEnum, List<Object>> mappedResponse = createVerifiedUser(createVerifiedUser(false));

            assertEquals(1, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                         "1 User should be errored"
            );
        }

        @Test
        void testCreateMultipleErroredUsers() throws Exception {
            Map<CreationEnum, List<Object>> mappedResponse = createVerifiedUser(createVerifiedUser(false),
                                                                            createVerifiedUser(false));

            assertEquals(2, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                         "2 Users should be errored"
            );
        }

        @Test
        void testCreateMultipleUsersCreateAndErrored() throws Exception {
            PiUser invalidUser = createVerifiedUser(false);

            Map<CreationEnum, List<Object>> mappedResponse = createVerifiedUser(verifiedUser, invalidUser);

            assertEquals(1, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                         "1 User should be created"
            );
            assertEquals(1, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                         "1 User should be errored"
            );
        }

        @Test
        void testErroredAccountWhenCreatingAVerifiedSsoUser() throws Exception {
            superAdminUser.setRoles(Roles.VERIFIED);

            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(superAdminUser)))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID)
                .contentType(MediaType.APPLICATION_JSON);

            MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(status().isCreated()).andReturn();
            Map<CreationEnum, List<Object>> mappedResponse =
                OBJECT_MAPPER.readValue(
                    response.getResponse().getContentAsString(),
                    new TypeReference<>() {
                    }
                );

            assertEquals(1, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                         "1 User should be errored"
            );
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedCreateAccount() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(verifiedUser)))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID)
                .contentType(MediaType.APPLICATION_JSON);

            assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
        }

        @Test
        void testUnauthorizedCreateThirdPartyUser() throws Exception {
            PiUser thirdPartyUser = createThirdPartyUser();
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(thirdPartyUser)))
                .header(REQUESTER_ID_HEADER, SUPER_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

            assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
        }
    }

    @Nested
    class GetUserByIdTests {

        @Test
        void testGetUserById() throws Exception {
            String createdUserId = createTestUserVerifiedAccount(verifiedUser);

            MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
                .get(ROOT_URL + "/" + createdUserId)
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID);

            MvcResult responseGetUser = mockMvc.perform(getRequest).andExpect(status().isOk()).andReturn();

            PiUser returnedUser = OBJECT_MAPPER.readValue(
                responseGetUser.getResponse().getContentAsString(),
                PiUser.class
            );
            assertEquals(createdUserId, returnedUser.getUserId().toString(), USER_SHOULD_MATCH);
        }

        @Test
        void testGetUserByIdNotFound() throws Exception {
            assertRequestResponseStatus(mockMvc, MockMvcRequestBuilders
                .get(ROOT_URL + "/" + UUID.randomUUID())
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID), NOT_FOUND.value());
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedGetUserById() throws Exception {
            assertRequestResponseStatus(mockMvc, MockMvcRequestBuilders
                .get(ROOT_URL + "/" + UUID.randomUUID())
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID), FORBIDDEN.value());
        }
    }

    @Nested
    class GetUserByProvenanceIdTests {

        @Test
        void testGetUserByProvenanceIdReturnsUser() throws Exception {
            createVerifiedUser(verifiedUser);

            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .get(String.format(REPLACE_STRING, GET_PROVENANCE_USER_URL, verifiedUser.getUserProvenance(),
                                   verifiedUser.getProvenanceUserId()
                ))
                .contentType(MediaType.APPLICATION_JSON);

            MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
            PiUser returnedUser = OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                PiUser.class
            );
            assertEquals(
                verifiedUser.getProvenanceUserId(), returnedUser.getProvenanceUserId(),
                USER_SHOULD_MATCH
            );
            assertThat(returnedUser.getCreatedDate()).as("Created date must not be null").isNotNull();
        }

        @Test
        void testGetUserByProvenanceIdReturnsNotFound() throws Exception {
            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .get(String.format(REPLACE_STRING, GET_PROVENANCE_USER_URL, UserProvenances.CFT_IDAM, ID))
                .contentType(MediaType.APPLICATION_JSON);

            MvcResult response =
                mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isNotFound()).andReturn();
            assertEquals(404, response.getResponse().getStatus(), "Status codes should match");
            assertTrue(
                response.getResponse().getContentAsString().contains(ERROR_RESPONSE_USER_PROVENANCE),
                "Should contain error message"
            );
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedGetUserByProvenanceId() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .get(String.format(REPLACE_STRING, GET_PROVENANCE_USER_URL, UserProvenances.CFT_IDAM, ID))
                .contentType(MediaType.APPLICATION_JSON);

            assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
        }
    }

    @Nested
    class UpdateAccountTests {

        @Test
        void testUpdateAccountLastVerifiedDateSuccessful() throws Exception {
            createVerifiedUser(verifiedUser);

            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .put(UPDATE_ACCOUNT_URL + verifiedUser.getUserProvenance() + "/"
                         + verifiedUser.getProvenanceUserId())
                .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
                    "lastVerifiedDate", "2022-08-14T20:21:10.912Z")))
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                    "has been updated")));
        }

        @Test
        void testUpdateAccountLastSignedInDateSuccessful() throws Exception {
            createVerifiedUser(verifiedUser);

            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .put(UPDATE_ACCOUNT_URL + verifiedUser.getUserProvenance() + "/"
                         + verifiedUser.getProvenanceUserId())
                .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
                    "lastSignedInDate", "2022-08-14T20:21:10.912Z")))
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                    "has been updated")));
        }

        @Test
        void testUpdateAccountNotFound() throws Exception {
            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .put(UPDATE_ACCOUNT_URL + verifiedUser.getUserProvenance() + "/1234")
                .content(OBJECT_MAPPER.writeValueAsString(Map.of(
                    "lastSignedInDate", "2022-08-14T20:21:20.912Z")))
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString(
                    "User with supplied provenance id: 1234 could not be found")));
        }

        @Test
        void testUpdateAccountWithUnsupportedParam() throws Exception {
            createVerifiedUser(verifiedUser);

            MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
                .put(UPDATE_ACCOUNT_URL + verifiedUser.getUserProvenance() + "/"
                         + verifiedUser.getProvenanceUserId())
                .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
                    "email", "test@test.com")))
                .contentType(MediaType.APPLICATION_JSON);

            mockMvc.perform(mockHttpServletRequestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("The field 'email' could not be updated")));
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedUpdateAccount() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put(UPDATE_ACCOUNT_URL + verifiedUser.getUserProvenance() + "/"
                         + verifiedUser.getProvenanceUserId())
                .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
                    "email", "test@test.com")))
                .contentType(MediaType.APPLICATION_JSON);

            assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
        }
    }

    @Nested
    class DeleteUserAccountV2Tests {

        @Test
        void testV2SystemAdminDeletesVerifiedUser() throws Exception {
            verifiedUser.setUserProvenance(UserProvenances.CFT_IDAM);

            MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + createTestUserVerifiedAccount(verifiedUser))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID);

            MvcResult mvcResult = mockMvc.perform(deleteRequest).andExpect(status().isOk()).andReturn();
            assertEquals(DELETE_USER_SUCCESS, mvcResult.getResponse().getContentAsString(),
                         DELETE_USER_FAILURE
            );
        }

        @Test
        void testV2SystemAdminDeletesThirdPartyUser() throws Exception {
            PiUser thirdPartyUser = createThirdPartyUser();

            MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + createTestThirdPartyUserValidAccount(thirdPartyUser))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID);

            MvcResult mvcResult = mockMvc.perform(deleteRequest).andExpect(status().isOk()).andReturn();
            assertEquals(DELETE_USER_SUCCESS, mvcResult.getResponse().getContentAsString(),
                         DELETE_USER_FAILURE
            );
        }

        @Test
        void testV2SystemAdminDeletesSuperAdminUser() throws Exception {
            String superAdminUserId = getSuperAdminUserId(superAdminUser);
            String systemAdminUserId = getSystemAdminUserId();

            MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + superAdminUserId)
                .header(REQUESTER_ID_HEADER, systemAdminUserId);

            MvcResult mvcResult = mockMvc.perform(deleteRequest).andExpect(status().isOk()).andReturn();
            assertEquals(DELETE_USER_SUCCESS, mvcResult.getResponse().getContentAsString(),
                         DELETE_USER_FAILURE
            );
        }

        @Test
        void testV2SuperAdminCannotDeleteUserWhenNotOwnAccount() throws Exception {
            verifiedUser.setUserProvenance(UserProvenances.CFT_IDAM);

            String superAdminUserId = getSuperAdminUserId(superAdminUser);

            MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + createTestUserVerifiedAccount(verifiedUser))
                .header(REQUESTER_ID_HEADER, superAdminUserId);

            mockMvc.perform(deleteRequest).andExpect(status().isForbidden()).andReturn();
        }

        @Test
        void testV2AdminDeletesOwnUser() throws Exception {
            String superAdminUserIdToDelete = getSuperAdminUserId(createAdminUser(true,
                                                                                  Roles.INTERNAL_SUPER_ADMIN_CTSC));

            MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + superAdminUserIdToDelete)
                .header(REQUESTER_ID_HEADER, superAdminUserIdToDelete);

            MvcResult mvcResult = mockMvc.perform(deleteRequest).andExpect(status().isOk()).andReturn();
            assertEquals(DELETE_USER_SUCCESS, mvcResult.getResponse().getContentAsString(),
                         DELETE_USER_FAILURE);
        }

        @Test
        void testV2DeleteAccountNotFound() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + USER_ID)
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID);

            assertRequestResponseStatus(mockMvc, request, NOT_FOUND.value());
        }

        @Test
        void testUnauthorizedDeleteAccountV2WhenNoUserId() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + USER_ID);

            assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedDeleteAccountV2() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .delete(ROOT_URL + DELETE_PATH_V2 + USER_ID)
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID);

            assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
        }
    }

    @Nested
    class UpdateAccountRoleTests {

        @Test
        void testUpdateAccountRoleByIdWithoutAdminIdReturnsForbiddenWhenNonSso() throws Exception {
            verifiedUser.setUserProvenance(UserProvenances.CFT_IDAM);

            MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
                .put(ROOT_URL + UPDATE_PATH + createTestUserVerifiedAccount(verifiedUser)
                         + "/" + Roles.INTERNAL_ADMIN_LOCAL);

            mockMvc.perform(updateRequest)
                .andExpect(status().isForbidden());
        }


        @Test
        void testUpdateAccountRoleByIdWithoutAdminIdReturnsOkWhenSso() throws Exception {
            PiUser superAdminUser = createAdminUser(true, Roles.INTERNAL_ADMIN_LOCAL);
            String superAdminUserId = getSuperAdminUserId(superAdminUser);

            MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
                .put(ROOT_URL + UPDATE_PATH + superAdminUserId
                         + "/" + Roles.INTERNAL_ADMIN_LOCAL);

            MvcResult updateResponse = mockMvc.perform(updateRequest).andExpect(status().isOk()).andReturn();

            assertEquals(
                "User with ID " + superAdminUserId + " has been updated to a " + Roles.INTERNAL_ADMIN_LOCAL,
                updateResponse.getResponse().getContentAsString(), "Failed to update account"
            );
        }

        @Test
        void testUpdateAccountRoleByIdWithAdminIdReturnsForbiddenWhenNonSso() throws Exception {
            verifiedUser.setUserProvenance(UserProvenances.CFT_IDAM);

            MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
                .put(ROOT_URL + UPDATE_PATH + createTestUserVerifiedAccount(verifiedUser)
                         + "/" + Roles.INTERNAL_ADMIN_LOCAL)
                .header(REQUESTER_ID_HEADER, SUPER_ADMIN_ISSUER_ID);

            mockMvc.perform(updateRequest)
                .andExpect(status().isForbidden());
        }


        @Test
        void testUpdateAccountRoleByIdWithAdminIdReturnsForbiddenWhenSso() throws Exception {
            PiUser superAdminUser = createAdminUser(true, Roles.INTERNAL_ADMIN_LOCAL);
            String superAdminUserId = getSuperAdminUserId(superAdminUser);

            MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
                .put(ROOT_URL + UPDATE_PATH + superAdminUserId
                         + "/" + Roles.INTERNAL_ADMIN_LOCAL)
                .header(REQUESTER_ID_HEADER, SUPER_ADMIN_ISSUER_ID);

            mockMvc.perform(updateRequest)
                .andExpect(status().isForbidden());
        }

        @Test
        void testUpdateAccountRoleByIdNotFound() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put(ROOT_URL + UPDATE_PATH + UUID.randomUUID() + "/" + Roles.INTERNAL_ADMIN_LOCAL);

            assertRequestResponseStatus(mockMvc, request, NOT_FOUND.value());
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedUpdateAccountRole() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .put(ROOT_URL + UPDATE_PATH + SUPER_ADMIN_ISSUER_ID + "/" + Roles.INTERNAL_ADMIN_LOCAL);

            assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
        }
    }

    private String getSuperAdminUserId(PiUser superAdminUser) throws Exception {
        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(superAdminUser)))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();

        Map<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        return mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).getFirst().toString();
    }

    private String getSystemAdminUserId() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(FIRST_NAME);
        systemAdmin.setSurname(SURNAME);
        systemAdmin.setEmail("test_account_system-admin2@hmcts.net");
        systemAdmin.setProvenanceUserId(UUID.randomUUID().toString());

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isOk()).andReturn();

        PiUser returnedUser = OBJECT_MAPPER.readValue(
            responseCreateSystemAdminUser.getResponse().getContentAsString(),
            PiUser.class
        );

        return returnedUser.getUserId().toString();
    }

    private void createAndAssertTestUser(int expectedSize, PiUser... piUser) throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(piUser)))
            .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        Map<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(expectedSize, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "User(s) should be created"
        );
    }

    private void createAndAssertVerifiedTestUser(int expectedSize, PiUser... piUser) throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(piUser)))
            .header(REQUESTER_ID_HEADER, SUPER_ADMIN_ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        Map<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(expectedSize, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "User(s) should be created"
        );
    }

    private Map<CreationEnum, List<Object>> createTestThirdPartyUser(PiUser... piUser) throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(piUser)))
            .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();

        return OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {});
    }

    private Map<CreationEnum, List<Object>> createVerifiedUser(PiUser... piUser) throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(piUser)))
            .header(REQUESTER_ID_HEADER, SUPER_ADMIN_ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();

        return OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), new TypeReference<>() {});
    }

    private PiUser createVerifiedUser(boolean valid) {
        return createUser(valid, UserProvenances.PI_AAD, Roles.VERIFIED);
    }

    private PiUser createAdminUser(boolean valid, Roles role) {
        return createUser(valid, UserProvenances.SSO, role);
    }

    private String createTestUserVerifiedAccount(PiUser piUser) throws Exception {
        return createVerifiedUser(piUser).get(CreationEnum.CREATED_ACCOUNTS).getFirst().toString();
    }

    private String createTestThirdPartyUserValidAccount(PiUser piUser) throws Exception {
        return createTestThirdPartyUser(piUser).get(CreationEnum.CREATED_ACCOUNTS).getFirst().toString();
    }

}
