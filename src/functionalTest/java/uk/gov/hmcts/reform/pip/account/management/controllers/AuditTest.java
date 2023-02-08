package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.*;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.JUnitTestsShouldIncludeAssert"})
public class AuditTest {
    @Autowired
    private MockMvc mockMvc;

    private static final String ROOT_URL = "/audit";
    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String AUDIT_DETAILS = "User requested to view all third party users";
    private static final String USER_ID = "1234";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeEach
    void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    private AuditLogDto createAuditLogDto() {
        AuditLogDto log = new AuditLogDto(
            USER_ID,
            EMAIL,
            AuditAction.MANAGE_THIRD_PARTY_USER_VIEW.toString(),
            AUDIT_DETAILS
        );
        return log;
    }

    @Test
    void testGetAllAuditLogs() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLogDto()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk());

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL))
            .andExpect(status().isOk())
            .andReturn();


        PageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );
       // AuditLog auditLog = pageResponse.getContent().get(0);

    }


    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetAllAuditLogs() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL))
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testCreateAuditLog() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLogDto()))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        AuditLog auditLog = OBJECT_MAPPER.readValue(
            mvcResult.getResponse()
                .getContentAsString(),
            AuditLog.class
        );

        assertEquals(EMAIL, auditLog.getUserEmail(), "Failed to create audit log");
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedCreateAuditLog() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLogDto()))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testDeleteAuditLogs() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLogDto()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk());


        MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
            .delete(ROOT_URL);

        MvcResult mvcResult = mockMvc.perform(deleteRequest).andExpect(status().isOk()).andReturn();
        assertEquals(
            "Audit logs that met the max retention period have been deleted",
            mvcResult.getResponse().getContentAsString(),
            "Failed to delete audit log"
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDeleteAuditLogs() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .delete(ROOT_URL);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
