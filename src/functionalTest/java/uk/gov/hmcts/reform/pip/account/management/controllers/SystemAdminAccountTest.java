package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class SystemAdminAccountTest {
    private static final String ROOT_URL = "/account";
    private static final String CREATE_SYSTEM_ADMIN_URL = ROOT_URL + "/add/system-admin";
    private static final String AZURE_PATH = "/azure/";

    private static final String ISSUER_ID = "1234-1234-1234-1234";
    private static final String SYSTEM_ADMIN_ISSUER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba2";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String GIVEN_NAME = "Given Name";
    private static final String ID = "1234";
    private static final String TEST_SYS_ADMIN_SURNAME = "testSysAdminSurname";
    private static final String TEST_SYS_ADMIN_FIRSTNAME = "testSysAdminFirstname";
    private static final String TEST_SYS_ADMIN_EMAIL = "testSysAdminEmail@justice.gov.uk";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    GraphServiceClient<Request> graphClient;

    @Autowired
    UserCollectionRequestBuilder userCollectionRequestBuilder;

    @Autowired
    UserCollectionRequest userCollectionRequest;

    @Autowired
    GraphServiceException graphServiceException;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void setup() {
        User user = new User();
        user.id = ID;
        user.givenName = GIVEN_NAME;

        List<User> azUsers = new ArrayList<>();
        azUsers.add(user);
        UserCollectionPage userCollectionPage = new UserCollectionPage(azUsers, userCollectionRequestBuilder);

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(user);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);
    }

    @AfterEach
    public void reset() {
        Mockito.reset(graphClient, userCollectionRequest, userCollectionRequestBuilder);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-system-admin.sql")
    void testCreateSystemAdminAccount() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(ISSUER_HEADER, SYSTEM_ADMIN_ISSUER_ID)
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
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-system-admin.sql")
    void testCreateSystemAdminAccountRequestExceeded() throws Exception {
        SystemAdminAccount systemAdmin1 = new SystemAdminAccount();
        systemAdmin1.setFirstName("testSysAdminFirstname1");
        systemAdmin1.setSurname("testSysAdminSurname1");
        systemAdmin1.setEmail("testSysAdminEmai1l@justice.gov.uk");

        MockHttpServletRequestBuilder createRequest1 =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin1))
                .header(ISSUER_HEADER, SYSTEM_ADMIN_ISSUER_ID)
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
                .header(ISSUER_HEADER, SYSTEM_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest2)
            .andExpect(status().isBadRequest()).andReturn();

        assertEquals(BAD_REQUEST.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     "Number of system admin accounts exceeded the max limit"
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-system-admin.sql")
    void testGetAzureUserInfo() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(ISSUER_HEADER, SYSTEM_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isOk()).andReturn();

        PiUser returnedUser = OBJECT_MAPPER.readValue(
            responseCreateSystemAdminUser.getResponse().getContentAsString(),
            PiUser.class
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
    @WithMockUser(username = "unauthroized_user", authorities = {"APPROLE_unknown.user"})
    void testUnauthorizedCreateSystemAdminAccount() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
