package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.model.CustomPageImpl;
import uk.gov.hmcts.reform.pip.account.management.model.account.AuditLog;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.AccountAuthorisationService;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class AuditTest {
    @Autowired
    private MockMvc mockMvc;

    private static final String ROOT_URL = "/audit";
    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String ADDITIONAL_USER_EMAIL = "test_account_admin_2@hmcts.net";
    private static final Roles ROLES = Roles.SYSTEM_ADMIN;
    private static final UserProvenances USER_PROVENANCE = UserProvenances.PI_AAD;
    private static final String AUDIT_DETAILS = "User requested to view all third party users";
    private static final String USER_ID = "1234";
    private static final String ADDITIONAL_USER_ID = "3456";
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final String REQUESTER_HEADER = "x-requester-id";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String GET_AUDIT_LOG_FAILED = "Failed to retrieve audit log";
    private static final String CREATE_AUDIT_LOG_FAILED = "Failed to create audit log";
    private static final AuditAction AUDIT_ACTION = AuditAction.MANAGE_THIRD_PARTY_USER_VIEW;
    private static final AuditAction ADDITIONAL_USER_AUDIT_ACTION = AuditAction.MANAGE_USER;
    private static final String ADDITIONAL_USER_AUDIT_DETAILS = "Manage user";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @MockitoBean
    private AccountAuthorisationService accountAuthorisationService;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void beforeEach() {
        when(accountAuthorisationService.userCanViewAuditLogs(any())).thenReturn(true);
    }

    private AuditLog createAuditLog() {
        return new AuditLog(
            USER_ID,
            EMAIL,
            ROLES,
            USER_PROVENANCE,
            AUDIT_ACTION,
            AUDIT_DETAILS
        );
    }

    @Test
    void testGetAllAuditLogs() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder1 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder1).andExpect(status().isOk());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder2 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(new AuditLog(
                ADDITIONAL_USER_ID,
                EMAIL,
                ROLES,
                USER_PROVENANCE,
                AUDIT_ACTION,
                AUDIT_DETAILS
            )))
            .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockHttpServletRequestBuilder2).andExpect(status().isOk());

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL).header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        CustomPageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );
        AuditLog auditLog1 = pageResponse.getContent().getFirst();

        assertEquals(EMAIL, auditLog1.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_ID, auditLog1.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog1.getDetails(), GET_AUDIT_LOG_FAILED);

        AuditLog auditLog2 = pageResponse.getContent().get(1);

        assertEquals(EMAIL, auditLog2.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(USER_ID, auditLog2.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog2.getDetails(), GET_AUDIT_LOG_FAILED);
    }

    @Test
    void testGetAllAuditLogsFilterByEmail() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder1 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder1).andExpect(status().isOk());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder2 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(new AuditLog(
                ADDITIONAL_USER_ID,
                ADDITIONAL_USER_EMAIL,
                ROLES,
                USER_PROVENANCE,
                AUDIT_ACTION,
                AUDIT_DETAILS
            )))
            .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockHttpServletRequestBuilder2).andExpect(status().isOk());

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL + "?email=" + EMAIL)
            .header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        CustomPageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );
        AuditLog auditLog1 = pageResponse.getContent().getFirst();

        assertEquals(EMAIL, auditLog1.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(USER_ID, auditLog1.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog1.getDetails(), GET_AUDIT_LOG_FAILED);
    }

    @Test
    void testGetAllAuditLogsPartialEmailFilter() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder1 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder1).andExpect(status().isOk());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder2 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(new AuditLog(
                ADDITIONAL_USER_ID,
                EMAIL,
                ROLES,
                USER_PROVENANCE,
                AUDIT_ACTION,
                AUDIT_DETAILS
            )))
            .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockHttpServletRequestBuilder2).andExpect(status().isOk());

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL + "?email=test_account_admin")
            .header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        CustomPageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        AuditLog auditLog1 = pageResponse.getContent().getFirst();

        assertEquals(EMAIL, auditLog1.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_ID, auditLog1.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog1.getDetails(), GET_AUDIT_LOG_FAILED);

        AuditLog auditLog2 = pageResponse.getContent().get(1);

        assertEquals(EMAIL, auditLog2.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(USER_ID, auditLog2.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog2.getDetails(), GET_AUDIT_LOG_FAILED);
    }

    @Test
    void testGetAllAuditLogsFilterByUserId() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder1 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder1).andExpect(status().isOk());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder2 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(new AuditLog(
                ADDITIONAL_USER_ID,
                ADDITIONAL_USER_EMAIL,
                ROLES,
                USER_PROVENANCE,
                AUDIT_ACTION,
                AUDIT_DETAILS
            )))
            .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockHttpServletRequestBuilder2).andExpect(status().isOk());

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL + "?userId=" + ADDITIONAL_USER_ID)
            .header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        CustomPageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );
        AuditLog auditLog1 = pageResponse.getContent().getFirst();

        assertEquals(ADDITIONAL_USER_EMAIL, auditLog1.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_ID, auditLog1.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog1.getDetails(), GET_AUDIT_LOG_FAILED);
    }

    @Test
    void testGetAllAuditLogsFilterByAuditAction() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder1 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder1).andExpect(status().isOk());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder2 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(new AuditLog(
                ADDITIONAL_USER_ID,
                ADDITIONAL_USER_EMAIL,
                ROLES,
                USER_PROVENANCE,
                ADDITIONAL_USER_AUDIT_ACTION,
                ADDITIONAL_USER_AUDIT_DETAILS
            )))
            .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockHttpServletRequestBuilder2).andExpect(status().isOk());

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL + "?actions=" + ADDITIONAL_USER_AUDIT_ACTION)
            .header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        CustomPageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );
        AuditLog auditLog1 = pageResponse.getContent().getFirst();


        assertEquals(1, pageResponse.getContent().size(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_EMAIL, auditLog1.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_ID, auditLog1.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_AUDIT_DETAILS, auditLog1.getDetails(), GET_AUDIT_LOG_FAILED);
    }

    @Test
    void testGetAllAuditLogsFilterByDate() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder1 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder1).andExpect(status().isOk());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder2 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(new AuditLog(
                ADDITIONAL_USER_ID,
                ADDITIONAL_USER_EMAIL,
                ROLES,
                USER_PROVENANCE,
                ADDITIONAL_USER_AUDIT_ACTION,
                ADDITIONAL_USER_AUDIT_DETAILS
            )))
            .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockHttpServletRequestBuilder2).andExpect(status().isOk());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate filterDate = LocalDate.parse(LocalDate.now().toString(), formatter);

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL + "?filterDate=" + filterDate)
            .header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        CustomPageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        AuditLog auditLog1 = pageResponse.getContent().getFirst();

        assertEquals(ADDITIONAL_USER_EMAIL, auditLog1.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_ID, auditLog1.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_AUDIT_DETAILS, auditLog1.getDetails(), GET_AUDIT_LOG_FAILED);

        AuditLog auditLog2 = pageResponse.getContent().get(1);

        assertEquals(EMAIL, auditLog2.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(USER_ID, auditLog2.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog2.getDetails(), GET_AUDIT_LOG_FAILED);
    }

    @Test
    void testGetAllAuditLogsFilterByEmailAndUserIdAndAuditActionAndDate() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder1 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder1).andExpect(status().isOk());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder2 = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(new AuditLog(
                ADDITIONAL_USER_ID,
                ADDITIONAL_USER_EMAIL,
                ROLES,
                USER_PROVENANCE,
                ADDITIONAL_USER_AUDIT_ACTION,
                ADDITIONAL_USER_AUDIT_DETAILS
            )))
            .contentType(MediaType.APPLICATION_JSON);
        mockMvc.perform(mockHttpServletRequestBuilder2).andExpect(status().isOk());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate filterDate = LocalDate.parse(LocalDate.now().toString(), formatter);

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL + "?email=" + ADDITIONAL_USER_EMAIL
             + "&userId=" + ADDITIONAL_USER_ID + "&actions=" + ADDITIONAL_USER_AUDIT_ACTION
                + "&filterDate=" + filterDate).header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        CustomPageImpl<AuditLog> pageResponse =
            OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );
        AuditLog auditLog1 = pageResponse.getContent().getFirst();

        assertEquals(ADDITIONAL_USER_EMAIL, auditLog1.getUserEmail(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_ID, auditLog1.getUserId(), GET_AUDIT_LOG_FAILED);
        assertEquals(ADDITIONAL_USER_AUDIT_DETAILS, auditLog1.getDetails(), GET_AUDIT_LOG_FAILED);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetAllAuditLogs() throws Exception {
        when(accountAuthorisationService.userCanViewAuditLogs(REQUESTER_ID)).thenReturn(false);

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL).header(REQUESTER_HEADER, REQUESTER_ID))
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testCreateAuditLog() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        AuditLog auditLog = OBJECT_MAPPER.readValue(
            mvcResult.getResponse()
                .getContentAsString(),
            AuditLog.class
        );

        assertEquals(EMAIL, auditLog.getUserEmail(), CREATE_AUDIT_LOG_FAILED);
        assertEquals(USER_ID, auditLog.getUserId(), CREATE_AUDIT_LOG_FAILED);
        assertEquals(AUDIT_DETAILS, auditLog.getDetails(), CREATE_AUDIT_LOG_FAILED);
    }

    @Test
    void testCreateAuditLogWhenAuditDetailLengthIsBelowMinimum() throws Exception {
        AuditLog belowMinimumAuditLog = new AuditLog(
            USER_ID,
            EMAIL,
            ROLES,
            USER_PROVENANCE,
            AUDIT_ACTION,
            ""
        );

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(belowMinimumAuditLog))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains(
            "details should be between 1 and 255 characters"),
                   "Audit log details should be between 1 and 255 characters");
    }

    @Test
    void testCreateAuditLogWhenAuditDetailLengthIsNull() throws Exception {
        AuditLog belowMinimumAuditLog = new AuditLog(
            USER_ID,
            EMAIL,
            ROLES,
            USER_PROVENANCE,
            AUDIT_ACTION,
            null
        );

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(belowMinimumAuditLog))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains("details must be provided"),
                   "The audit details field must be provided");
    }

    @Test
    void testCreateAuditLogWhenAuditDetailLengthIsAboveMaximum() throws Exception {
        AuditLog aboveMaximumAuditLog = new AuditLog(
            USER_ID,
            EMAIL,
            ROLES,
            USER_PROVENANCE,
            AUDIT_ACTION,
            RandomStringUtils.secure().next(256, true, false)
        );

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(aboveMaximumAuditLog))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains(
            "details should be between 1 and 255 characters"),
                   "Audit log details should be between 1 and 255 characters");
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedCreateAuditLog() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testDeleteAuditLogs() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ROOT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(createAuditLog()))
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

        MvcResult mvcResult = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
