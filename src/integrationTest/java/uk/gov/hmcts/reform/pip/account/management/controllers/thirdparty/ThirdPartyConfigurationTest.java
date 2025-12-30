package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.ThirdPartyAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class ThirdPartyConfigurationTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String THIRD_PARTY_CONFIGURATION_PATH = "/third-party/configuration";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final UUID REQUESTER_ID = UUID.randomUUID();

    private static ApiUser apiUser = new ApiUser();
    private static ApiOauthConfiguration apiOauthConfiguration = new ApiOauthConfiguration();

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    private ThirdPartyAuthorisationService thirdPartyAuthorisationService;

    @BeforeAll
    static void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    public void setupEach() {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(true);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedCreateThirdPartyConfiguration() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_CONFIGURATION_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiOauthConfiguration));

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedGetThirdPartyConfigurationByUserId() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_CONFIGURATION_PATH + "/", UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedUpdateThirdPartyConfiguration() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(THIRD_PARTY_CONFIGURATION_PATH + "/", UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiOauthConfiguration));

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }
}
