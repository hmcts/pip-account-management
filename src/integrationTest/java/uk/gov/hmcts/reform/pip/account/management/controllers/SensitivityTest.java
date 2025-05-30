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
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = { "APPROLE_api.request.admin" })
class SensitivityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ROOT_URL = "/account";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String ISSUER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba2";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String EMAIL = "a@b.com";
    private static final String URL_FORMAT = "%s/isAuthorised/%s/%s/%s";

    private static final String TRUE_MESSAGE = "Should return true";
    private static final String FALSE_MESSAGE = "Should return false";

    private static final String SQL_SCRIPT = "classpath:add-admin-users.sql";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PiUser user;

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private String createUserAndGetId(PiUser validUser) throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult userResponse = mockMvc.perform(setupRequest).andExpect(status().isCreated()).andReturn();
        Map<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(userResponse.getResponse().getContentAsString(),
                                   new TypeReference<>() {});
        return mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();
    }

    @BeforeEach
    public void setup() {
        user = new PiUser();
        user.setEmail(EMAIL);
        user.setProvenanceUserId(UUID.randomUUID().toString());
    }

    @Test
    void testIsUserAuthenticatedReturnsTrueWhenPublicListAndVerified() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.VERIFIED);

        MvcResult response = callIsAuthorised(user, Sensitivity.PUBLIC);
        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsTrueWhenPublicListAndAdmin() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        MvcResult response = callIsAuthorised(user, Sensitivity.PUBLIC);
        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_SCRIPT)
    void testIsUserAuthenticatedReturnsTrueWhenPublicListAndThirdParty() throws Exception {
        user.setUserProvenance(UserProvenances.THIRD_PARTY);
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_ALL);

        MvcResult response = callIsAuthorised(user, Sensitivity.PUBLIC);
        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsTrueWhenPrivateListAndVerified() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.VERIFIED);

        MvcResult response = callIsAuthorised(user, Sensitivity.PRIVATE);
        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsFalseWhenPrivateListAndAdmin() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        MvcResult response = callIsAuthorised(user, Sensitivity.PRIVATE);
        assertFalse(Boolean.parseBoolean(response.getResponse().getContentAsString()), FALSE_MESSAGE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_SCRIPT)
    void testIsUserAuthenticatedReturnsTrueWhenPrivateListAndThirdParty() throws Exception {
        user.setUserProvenance(UserProvenances.THIRD_PARTY);
        user.setRoles(Roles.GENERAL_THIRD_PARTY);

        MvcResult response = callIsAuthorised(user, Sensitivity.PRIVATE);
        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsTrueWhenClassifiedListAndVerifiedAndCorrectProvenance() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.VERIFIED);

        MvcResult response = callIsAuthorised(user, Sensitivity.CLASSIFIED);
        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsFalseWhenClassifiedListAndVerifiedAndIncorrectProvenance() throws Exception {
        user.setUserProvenance(UserProvenances.CFT_IDAM);
        user.setRoles(Roles.VERIFIED);

        MvcResult response = callIsAuthorised(user, Sensitivity.CLASSIFIED);
        assertFalse(Boolean.parseBoolean(response.getResponse().getContentAsString()), FALSE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsFalseWhenClassifiedListAndAdminAndCorrectProvenance() throws Exception {
        user.setUserProvenance(UserProvenances.CFT_IDAM);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        MvcResult response = callIsAuthorised(user, Sensitivity.CLASSIFIED);
        assertFalse(Boolean.parseBoolean(response.getResponse().getContentAsString()), FALSE_MESSAGE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_SCRIPT)
    void testIsUserAuthenticatedReturnsTrueWhenClassifiedListAndThirdPartyPressRole() throws Exception {
        user.setUserProvenance(UserProvenances.THIRD_PARTY);
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_PRESS);

        MvcResult response = callIsAuthorised(user, Sensitivity.CLASSIFIED);
        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = SQL_SCRIPT)
    void testIsUserAuthenticatedReturnsFalseWhenClassifiedListAndThirdPartyNonPressRole() throws Exception {
        user.setUserProvenance(UserProvenances.THIRD_PARTY);
        user.setRoles(Roles.VERIFIED_THIRD_PARTY_CRIME_CFT);

        MvcResult response = callIsAuthorised(user, Sensitivity.CLASSIFIED);
        assertFalse(Boolean.parseBoolean(response.getResponse().getContentAsString()), FALSE_MESSAGE);
    }

    @SuppressWarnings("PMD.SignatureDeclareThrowsException")
    private MvcResult callIsAuthorised(PiUser user, Sensitivity sensitivity) throws Exception {
        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST, sensitivity));

        return mockMvc.perform(request).andExpect(status().isOk()).andReturn();
    }
}
