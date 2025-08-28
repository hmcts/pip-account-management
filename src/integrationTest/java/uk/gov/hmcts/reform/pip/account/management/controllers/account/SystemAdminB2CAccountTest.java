package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class SystemAdminB2CAccountTest extends IntegrationTestBase {
    private static final String ROOT_URL = "/account";
    private static final String CREATE_SYSTEM_ADMIN_URL = ROOT_URL + "/add/system-admin";
    private static final String AZURE_PATH = "/azure/";

    private static final String SYSTEM_ADMIN_ISSUER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba2";
    private static final String SUPER_ADMIN_ISSUER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba3";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final String GIVEN_NAME = "Given Name";
    private static final String ID = "1234";
    private static final String TEST_SYS_ADMIN_SURNAME = "testSysAdminSurname";
    private static final String TEST_SYS_ADMIN_FIRSTNAME = "testSysAdminFirstname";
    private static final String TEST_SYS_ADMIN_EMAIL = "testSysAdminEmail@justice.gov.uk";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String ADD_USERS_SCRIPT = "classpath:add-admin-users.sql";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    GraphServiceClient graphClient;

    @Mock
    UsersRequestBuilder usersRequestBuilder;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void setup() {
        User user = new User();
        user.setId(ID);
        user.setGivenName(GIVEN_NAME);

        List<User> azUsers = new ArrayList<>();
        azUsers.add(user);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(user);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(azUsers);

        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_USERS_SCRIPT)
    void testCreateSystemAdminAccount() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isOk()).andReturn();

        PiUser returnedUser = OBJECT_MAPPER.readValue(
            responseCreateSystemAdminUser.getResponse().getContentAsString(),
            PiUser.class
        );

        assertEquals(
            systemAdmin.getEmail(), returnedUser.getEmail(),
            "Failed to create user"
        );

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(ROOT_URL + AZURE_PATH + returnedUser.getProvenanceUserId());

        MvcResult responseGetUser =
            mockMvc.perform(getRequest).andExpect(status().isOk()).andReturn();

        AzureAccount returnedAzureAccount = OBJECT_MAPPER.readValue(
            responseGetUser.getResponse().getContentAsString(),
            AzureAccount.class
        );
        assertEquals(returnedUser.getEmail(), returnedAzureAccount.getEmail(),
                     "Should return the correct user"
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_USERS_SCRIPT)
    void testCreateSystemAdminAccountRequestExceeded() throws Exception {
        SystemAdminAccount systemAdmin1 = new SystemAdminAccount();
        systemAdmin1.setFirstName("testSysAdminFirstname1");
        systemAdmin1.setSurname("testSysAdminSurname1");
        systemAdmin1.setEmail("testSysAdminEmai1l@justice.gov.uk");

        MockHttpServletRequestBuilder createRequest1 =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin1))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(createRequest1)
            .andExpect(status().isOk());

        SystemAdminAccount systemAdmin2 = new SystemAdminAccount();
        systemAdmin2.setFirstName("testSysAdminFirstname2");
        systemAdmin2.setSurname("testSysAdminSurname2");
        systemAdmin2.setEmail("testSysAdminEmai12@justice.gov.uk");

        MockHttpServletRequestBuilder createRequest2 =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin2))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest2)
            .andExpect(status().isBadRequest()).andReturn();

        assertEquals(BAD_REQUEST.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     "Number of system admin accounts exceeded the max limit"
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_USERS_SCRIPT)
    void testBadRequestCreateSystemAdminAccountIfNoAdminHeaderProvided() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isBadRequest()).andReturn();

        assertEquals(BAD_REQUEST.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     "No header admin header provided"
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_USERS_SCRIPT)
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedRequestCreateSystemAdmin() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);

        MockHttpServletRequestBuilder request =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
    }

    @Test
    void testCreateSystemAdminUserWhenNotSystemAdmin() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);

        MockHttpServletRequestBuilder request =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(REQUESTER_ID_HEADER, SUPER_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
    }
}
