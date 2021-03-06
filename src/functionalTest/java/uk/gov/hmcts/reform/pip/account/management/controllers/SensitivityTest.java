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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTest;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTest.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = "admin", authorities = { "APPROLE_api.request.admin" })
class SensitivityTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String ROOT_URL = "/account";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String ISSUER_ID = "abcde";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String EMAIL = "a@b.com";
    private static final String URL_FORMAT = "%s/isAuthorised/%s/%s/%s";

    private static final String TRUE_MESSAGE = "Should return true";
    private static final String FALSE_MESSAGE = "Should return false";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private PiUser user;

    private String createUserAndGetId(PiUser validUser) throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult userResponse = mockMvc.perform(setupRequest).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
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

        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST,
                               Sensitivity.PUBLIC
            ));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsTrueWhenPublicListAndAdmin() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST,
                               Sensitivity.PUBLIC
            ));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsTrueWhenPrivateListAndVerified() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.VERIFIED);

        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST,
                               Sensitivity.PRIVATE
            ));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsFalseWhenPrivateListAndAdmin() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST,
                               Sensitivity.PRIVATE
            ));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertFalse(Boolean.parseBoolean(response.getResponse().getContentAsString()), FALSE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsTrueWhenClassifiedListAndVerifiedAndCorrectProvenance() throws Exception {
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.VERIFIED);

        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST,
                               Sensitivity.CLASSIFIED
            ));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertTrue(Boolean.parseBoolean(response.getResponse().getContentAsString()), TRUE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsFalseWhenClassifiedListAndVerifiedAndIncorrectProvenance() throws Exception {
        user.setUserProvenance(UserProvenances.CFT_IDAM);
        user.setRoles(Roles.VERIFIED);

        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST,
                               Sensitivity.CLASSIFIED
            ));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertFalse(Boolean.parseBoolean(response.getResponse().getContentAsString()), FALSE_MESSAGE);
    }

    @Test
    void testIsUserAuthenticatedReturnsFalseWhenClassifiedListAndAdminAndCorrectProvenance() throws Exception {
        user.setUserProvenance(UserProvenances.CFT_IDAM);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);

        String createdUserId = createUserAndGetId(user);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format(URL_FORMAT, ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST,
                               Sensitivity.CLASSIFIED
            ));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertFalse(Boolean.parseBoolean(response.getResponse().getContentAsString()), FALSE_MESSAGE);
    }

}
