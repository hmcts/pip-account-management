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
class ThirdPartyUserTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UNAUTHORISED_ROLE = "APPROLE_unknown.authorised";
    private static final String UNAUTHORISED_USERNAME = "unauthorised_isAuthorised";

    private static final String THIRD_PARTY_USER_PATH = "/third-party";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final String USER_NAME = "ThirdPartyUser";

    private static ApiUser apiUser = new ApiUser();

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    private ThirdPartyAuthorisationService thirdPartyAuthorisationService;

    @BeforeAll
    static void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
        apiUser.setName(USER_NAME);
    }

    @BeforeEach
    void setupEach() {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(true);
    }

    @Test
    void testCreateThirdPartyUserSuccess() throws Exception {
        UUID createdUserId = OBJECT_MAPPER.readValue(createThirdPartyUser().getResponse().getContentAsString(),
                                                        UUID.class);

        assertThat(createdUserId.toString())
            .as("Created user ID should be returned")
            .isNotBlank();
    }

    @Test
    void testCreateThirdPartyUserBadRequest() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(new ApiUser()));

        mvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedCreateThirdPartyUser() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiUser));

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetAllThirdPartyUsersSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedGetAllThirdPartyUsers() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetThirdPartyUserByUserIdSuccess() throws Exception {
        UUID createdUserId = OBJECT_MAPPER.readValue(createThirdPartyUser().getResponse().getContentAsString(),
                                                     UUID.class);

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(THIRD_PARTY_USER_PATH + "/" + createdUserId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        MvcResult getResponse = mvc.perform(getRequest)
            .andExpect(status().isOk())
            .andReturn();

        ApiUser retrievedApiUser = OBJECT_MAPPER.readValue(getResponse.getResponse().getContentAsString(),
                                                           ApiUser.class);

        assertThat(retrievedApiUser.getName())
            .as("Retrieved user name should be returned")
            .isEqualTo(USER_NAME);
    }

    @Test
    void testGetThirdPartyUserNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_USER_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedGetThirdPartyUserByUserId() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_USER_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteThirdPartyUserSuccess() throws Exception {
        UUID createdUserId = OBJECT_MAPPER.readValue(createThirdPartyUser().getResponse().getContentAsString(),
                                                     UUID.class);

        MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
            .delete(THIRD_PARTY_USER_PATH + "/" + createdUserId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        MvcResult deleteResponse = mvc.perform(deleteRequest)
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Response message should confirm deletion")
            .isEqualTo(String.format("Third-party user with ID %s has been deleted", createdUserId));
    }

    @Test
    void testDeleteThirdPartyUserNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(THIRD_PARTY_USER_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedDeleteThirdPartyUser() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(THIRD_PARTY_USER_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    private MvcResult createThirdPartyUser() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiUser));

        return mvc.perform(request)
            .andExpect(status().isCreated())
            .andReturn();
    }
}
