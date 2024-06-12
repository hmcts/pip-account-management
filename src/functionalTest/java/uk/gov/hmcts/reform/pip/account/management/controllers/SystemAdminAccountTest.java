package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    private static final String CREATE_SYSTEM_ADMIN_URL = ROOT_URL + "/system-admin";

    private static final String SYSTEM_ADMIN_ISSUER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba2";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String TEST_SYS_ADMIN_SURNAME = "testSysAdminSurname";
    private static final String TEST_SYS_ADMIN_FIRSTNAME = "testSysAdminFirstname";
    private static final String TEST_SYS_ADMIN_EMAIL = "testSysAdminEmail@justice.gov.uk";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String SQL_ADD_ADMIN = "classpath:add-admin-users.sql";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_ADD_ADMIN)
    void testUserCanCreateSystemAdminAccount() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);
        systemAdmin.setProvenanceUserId(UUID.randomUUID().toString());

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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_ADD_ADMIN)
    void testCreateSystemAdminAccountRequestExceeded() throws Exception {
        SystemAdminAccount systemAdmin1 = new SystemAdminAccount();
        systemAdmin1.setFirstName("testSysAdminFirstname1");
        systemAdmin1.setSurname("testSysAdminSurname1");
        systemAdmin1.setEmail("testSysAdminEmai1l@justice.gov.uk");
        systemAdmin1.setProvenanceUserId(UUID.randomUUID().toString());

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
        systemAdmin2.setProvenanceUserId(UUID.randomUUID().toString());

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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_ADD_ADMIN)
    void testUserCanCreateSystemAdminAccountBadRequest() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(ISSUER_HEADER, SYSTEM_ADMIN_ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isBadRequest()).andReturn();

        assertEquals(BAD_REQUEST.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     "Should return bad request"
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_ADD_ADMIN)
    void testUnauthorizedUserCanCreateSystemAdminAccount() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);
        systemAdmin.setProvenanceUserId(UUID.randomUUID().toString());

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .header(ISSUER_HEADER, "87f907d2-eb28-42cc-b6e1-ae2b03f7bba3")
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
