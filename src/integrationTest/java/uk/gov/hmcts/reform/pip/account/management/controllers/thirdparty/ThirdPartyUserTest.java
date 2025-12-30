package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
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

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class ThirdPartyUserTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
    public void setupEach() {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(true);
    }

    @Test
    void testCreateThirdPartyUserSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(apiUser));

        MvcResult result = mvc.perform(request)
            .andExpect(status().isCreated())
            .andReturn();

        assertThat(result.getResponse().getContentAsString())
            .contains("Third-party user created");
    }

    @Test
    void testCreateThirdPartyUserWithNoName() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_USER_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(new ApiUser()));

        mvc.perform(request)
            .andExpect(status().isBadRequest());
    }
}
