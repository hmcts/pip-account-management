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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.ThirdPartyAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class ThirdPartyConfigurationTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UNAUTHORISED_ROLE = "APPROLE_unknown.authorised";
    private static final String UNAUTHORISED_USERNAME = "unauthorised_isAuthorised";

    private static final String THIRD_PARTY_USER_PATH = "/third-party";
    private static final String THIRD_PARTY_CONFIGURATION_PATH = THIRD_PARTY_USER_PATH + "/configuration";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final UUID REQUESTER_ID = UUID.randomUUID();

    private static final String USER_NAME = "ThirdPartyUser";
    private static final String DESTINATION_URL = "https://example.com/callback";
    private static final String UPDATED_DESTINATION_URL = "https://example.com/callback-updated";
    private static final String TOKEN_URL = "https://example.com/token";
    private static final String CLIENT_ID_KEY = "client-id";
    private static final String CLIENT_SECRET_KEY = "client-secret";
    private static final String SCOPE_KEY = "scope";

    private ApiOauthConfiguration apiOauthConfiguration = new ApiOauthConfiguration();

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    private ThirdPartyAuthorisationService thirdPartyAuthorisationService;

    @BeforeAll
    static void setup() throws Exception {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void setupEach() {
        apiOauthConfiguration.setDestinationUrl(DESTINATION_URL);
        apiOauthConfiguration.setTokenUrl(TOKEN_URL);
        apiOauthConfiguration.setClientIdKey(CLIENT_ID_KEY);
        apiOauthConfiguration.setClientSecretKey(CLIENT_SECRET_KEY);
        apiOauthConfiguration.setScopeKey(SCOPE_KEY);

        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(true);
    }

    @Test
    void testCreateThirdPartyConfigurationSuccess() throws Exception {
        UUID userId = createApiUser();
        apiOauthConfiguration.setUserId(userId);

        assertThat(createApiOauthConfiguration().getResponse().getContentAsString())
            .as("Creation success message should be returned")
            .isEqualTo(
                String.format("Third-party OAuth configuration successfully created for user with ID %s", userId)
            );
    }

    @Test
    void testCreateThirdPartyConfigurationBadRequest() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_CONFIGURATION_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(new ApiOauthConfiguration()));

        mvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
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
    void testGetThirdPartyConfigurationByUserIdSuccess() throws Exception {
        UUID userId = createApiUser();
        apiOauthConfiguration.setUserId(userId);
        createApiOauthConfiguration();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(THIRD_PARTY_CONFIGURATION_PATH + "/" + userId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        MvcResult getResponse = mvc.perform(getRequest)
            .andExpect(status().isOk())
            .andReturn();

        ApiOauthConfiguration retrievedApiOauthConfiguration = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            ApiOauthConfiguration.class
        );

        assertThat(retrievedApiOauthConfiguration)
            .as("Retrieved API OAuth configuration should match the created one")
            .extracting(ApiOauthConfiguration::getDestinationUrl,
                        ApiOauthConfiguration::getTokenUrl,
                        ApiOauthConfiguration::getClientIdKey,
                        ApiOauthConfiguration::getClientSecretKey,
                        ApiOauthConfiguration::getScopeKey)
            .containsExactly(DESTINATION_URL, TOKEN_URL, CLIENT_ID_KEY, CLIENT_SECRET_KEY, SCOPE_KEY);
    }

    @Test
    void testGetThirdPartyConfigurationNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_CONFIGURATION_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedGetThirdPartyConfigurationByUserId() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_CONFIGURATION_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateThirdPartyConfigurationSuccess() throws Exception {
        UUID userId = createApiUser();
        apiOauthConfiguration.setUserId(userId);
        createApiOauthConfiguration();

        apiOauthConfiguration.setDestinationUrl(UPDATED_DESTINATION_URL);

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
            .put(THIRD_PARTY_CONFIGURATION_PATH + "/" + userId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiOauthConfiguration));

        mvc.perform(updateRequest)
            .andExpect(status().isOk());

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(THIRD_PARTY_CONFIGURATION_PATH + "/" + userId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        MvcResult getResponse = mvc.perform(getRequest)
            .andExpect(status().isOk())
            .andReturn();

        ApiOauthConfiguration updatedApiOauthConfiguration = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            ApiOauthConfiguration.class
        );

        assertThat(updatedApiOauthConfiguration.getDestinationUrl())
            .as("Destination URL should be updated")
            .isEqualTo(UPDATED_DESTINATION_URL);
    }

    @Test
    void testUpdateThirdPartyConfigurationBadRequestIfEmptyField() throws Exception {
        UUID userId = createApiUser();
        apiOauthConfiguration.setUserId(userId);
        createApiOauthConfiguration();

        apiOauthConfiguration.setDestinationUrl("");

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(THIRD_PARTY_CONFIGURATION_PATH + "/" + userId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiOauthConfiguration));

        mvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedUpdateThirdPartyConfiguration() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(THIRD_PARTY_CONFIGURATION_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiOauthConfiguration));

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    private UUID createApiUser() throws Exception {
        ApiUser apiUser = new ApiUser();
        apiUser.setName(USER_NAME);
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(true);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiUser));

        MvcResult response = mvc.perform(request)
            .andExpect(status().isCreated())
            .andReturn();

        ApiUser createdApiUser = OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), ApiUser.class);
        return createdApiUser.getUserId();
    }

    private MvcResult createApiOauthConfiguration() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_CONFIGURATION_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiOauthConfiguration));

        return mvc.perform(request)
            .andExpect(status().isCreated())
            .andReturn();
    }
}
