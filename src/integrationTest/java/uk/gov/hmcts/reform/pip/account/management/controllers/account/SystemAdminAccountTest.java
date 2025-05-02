package uk.gov.hmcts.reform.pip.account.management.controllers.account;

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
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class SystemAdminAccountTest {

    private static final String ROOT_URL = "/account";
    private static final String CREATE_SYSTEM_ADMIN_URL = ROOT_URL + "/system-admin";

    private static final String TEST_SYS_ADMIN_SURNAME = "testSysAdminSurname";
    private static final String TEST_SYS_ADMIN_FIRSTNAME = "testSysAdminFirstname";
    private static final String TEST_SYS_ADMIN_EMAIL = "testSysAdminEmail@justice.gov.uk";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @Test
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
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-admin-users.sql")
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
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest2)
            .andExpect(status().isBadRequest()).andReturn();

        assertEquals(BAD_REQUEST.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     "Number of system admin accounts exceeded the max limit"
        );
    }

    @Test
    void testCreateSystemAdminReturnsBadRequest() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isBadRequest()).andReturn();

        assertEquals(BAD_REQUEST.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     "Should return bad request"
        );
    }

    @Test
    @WithMockUser(username = "unauthroized_user", authorities = {"APPROLE_unknown.user"})
    void testUnauthorizedCreateSystemAdminAccount() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName(TEST_SYS_ADMIN_FIRSTNAME);
        systemAdmin.setSurname(TEST_SYS_ADMIN_SURNAME);
        systemAdmin.setEmail(TEST_SYS_ADMIN_EMAIL);
        systemAdmin.setProvenanceUserId(UUID.randomUUID().toString());

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(OBJECT_MAPPER.writeValueAsString(systemAdmin))
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
