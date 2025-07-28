package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomUtils;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@SuppressWarnings("PMD.TooManyMethods")
class CustomAccountRetrievalTest extends IntegrationTestBase {
    private static final String ROOT_URL = "/account";
    private static final String ADMIN_ROOT_URL = "/account/admin/";
    private static final String THIRD_PARTY_URL = ROOT_URL + "/all/third-party";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String GET_ALL_ACCOUNTS_EXCEPT_THIRD_PARTY = ROOT_URL + "/all";
    private static final String MI_REPORTING_ACCOUNT_DATA_URL = ROOT_URL + "/mi-data";

    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String INVALID_EMAIL = "ab";
    private static final String SURNAME = "Surname";
    private static final String FORENAME = "Forename";
    private static final String ISSUER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba2";
    private static final String ISSUER_HEADER = "x-issuer-id";

    private static final String USER_SHOULD_MATCH = "Users should match";

    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final PiUser VALID_USER = createUser(true, UUID.randomUUID().toString());

    @Autowired
    private MockMvc mockMvc;

    private static PiUser createUser(boolean valid, String id) {
        PiUser user = new PiUser();
        user.setEmail(valid ? EMAIL : INVALID_EMAIL);
        user.setProvenanceUserId(id);
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        user.setForenames(FORENAME);
        user.setSurname(SURNAME);
        return user;
    }

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @Test
    void testMiData() throws Exception {
        String provenanceId = UUID.randomUUID().toString();
        PiUser validUser = createUser(true, provenanceId);
        validUser.setEmail("test-account-am-" + RandomUtils.nextInt() + "@hmcts.net");

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        Map<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(MI_REPORTING_ACCOUNT_DATA_URL);

        MvcResult miDataResponse = mockMvc.perform(request).andExpect(status().isOk()).andReturn();
        List<AccountMiData> accountMiData =
            Arrays.stream(OBJECT_MAPPER.readValue(
                miDataResponse.getResponse().getContentAsString(), AccountMiData[].class)).toList();

        assertThat(accountMiData)
            .as("Returned account MI data must match user object")
            .anyMatch(account -> createdUserId.equals(account.getUserId().toString())
                && provenanceId.equals(account.getProvenanceUserId())
                && UserProvenances.PI_AAD.equals(account.getUserProvenance())
                && Roles.INTERNAL_ADMIN_CTSC.equals(account.getRoles())
                && account.getCreatedDate() != null
                && account.getLastSignedInDate() != null);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetMiData() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(MI_REPORTING_ACCOUNT_DATA_URL);

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-admin-users.sql")
    void testGetAllThirdPartyAccounts() throws Exception {
        VALID_USER.setProvenanceUserId("THIRD_PARTY");
        VALID_USER.setUserProvenance(UserProvenances.THIRD_PARTY);
        VALID_USER.setRoles(Roles.GENERAL_THIRD_PARTY);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(VALID_USER)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        Map<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(THIRD_PARTY_URL);

        MvcResult responseGetUser =
            mockMvc.perform(getRequest).andExpect(status().isOk()).andReturn();

        PiUser[] users = OBJECT_MAPPER.readValue(
            responseGetUser.getResponse().getContentAsString(),
            PiUser[].class
        );

        assertEquals(1, users.length, "Correct number of users should return");
        assertEquals(createdUserId, users[0].getUserId().toString(), USER_SHOULD_MATCH);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetAllThirdPartyAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_URL);

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
    }

    @Test
    void testGetAllAccountsExceptThirdParty() throws Exception {
        PiUser validUser1 = createUser(true, UUID.randomUUID().toString());
        PiUser validUser2 = createUser(true, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser1, validUser2)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);
        MvcResult responseCreateUser = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isCreated()).andReturn();

        Map<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(GET_ALL_ACCOUNTS_EXCEPT_THIRD_PARTY);

        MvcResult response =
            mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertTrue(
            response.getResponse().getContentAsString().contains(createdUserId),
            "Failed to get all accounts"
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetAllAccountsExceptThirdParty() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(GET_ALL_ACCOUNTS_EXCEPT_THIRD_PARTY);

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
    }

    @Test
    void testGetAdminUserByEmailAndProvenance() throws Exception {
        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(VALID_USER)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        Map<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(ADMIN_ROOT_URL + VALID_USER.getEmail() + "/" + VALID_USER.getUserProvenance());

        MvcResult responseGetUser =
            mockMvc.perform(getRequest).andExpect(status().isOk()).andReturn();

        PiUser returnedUser = OBJECT_MAPPER.readValue(
            responseGetUser.getResponse().getContentAsString(),
            PiUser.class
        );
        assertEquals(createdUserId, returnedUser.getUserId().toString(), "Should return the correct user");
    }

    @Test
    void testGetAdminUserByEmailAndProvenanceNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ADMIN_ROOT_URL + VALID_USER.getEmail() + "/" + UserProvenances.SSO);

        assertRequestResponseStatus(mockMvc, request, NOT_FOUND.value());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetAdminUserByEmailAndProvenance() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ADMIN_ROOT_URL + VALID_USER.getEmail() + "/" + VALID_USER.getUserProvenance());

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
    }
}
