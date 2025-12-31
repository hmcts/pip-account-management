package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.ThirdPartyAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThirdPartySubscriptionTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String UNAUTHORISED_ROLE = "APPROLE_unknown.authorised";
    private static final String UNAUTHORISED_USERNAME = "unauthorised_isAuthorised";

    private static final String THIRD_PARTY_USER_PATH = "/third-party";
    private static final String THIRD_PARTY_SUBSCRIPTION_PATH = THIRD_PARTY_USER_PATH + "/subscription";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final String USER_NAME = "ThirdPartyUser";

    private ApiSubscription apiSubscription1 = new ApiSubscription();
    private ApiSubscription apiSubscription2 = new ApiSubscription();

    @Autowired
    protected MockMvc mvc;

    @MockitoBean
    private ThirdPartyAuthorisationService thirdPartyAuthorisationService;

    @BeforeAll
    void setup() throws Exception {
        OBJECT_MAPPER.findAndRegisterModules();

        apiSubscription1.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);
        apiSubscription1.setSensitivity(Sensitivity.PUBLIC);
        apiSubscription2.setListType(ListType.FAMILY_DAILY_CAUSE_LIST);
        apiSubscription2.setSensitivity(Sensitivity.CLASSIFIED);
    }

    @BeforeEach
    void setupEach() {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(true);
    }

    @Test
    void testCreateThirdPartySubscriptionSuccess() throws Exception {
        UUID userId = createApiUser();
        apiSubscription1.setUserId(userId);
        apiSubscription2.setUserId(userId);

        assertThat(createApiSubscriptions().getResponse().getContentAsString())
            .as("Creation success message should be returned")
            .isEqualTo(String.format("Third-party subscriptions successfully created for user with ID %s", userId));
    }

    @Test
    void testCreateThirdPartySubscriptionBadRequestIfNoSubscriptionSupplied() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_SUBSCRIPTION_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(List.of()));

        mvc.perform(request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedCreateThirdPartyConfiguration() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_SUBSCRIPTION_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(apiSubscription1)));

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetThirdPartySubscriptionByUserIdSuccess() throws Exception {
        UUID userId = createApiUser();
        apiSubscription1.setUserId(userId);
        apiSubscription2.setUserId(userId);
        createApiSubscriptions();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(THIRD_PARTY_SUBSCRIPTION_PATH + "/" + userId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        MvcResult getResponse = mvc.perform(getRequest)
            .andExpect(status().isOk())
            .andReturn();

        List<ApiSubscription> retrievedApiSubscriptions = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            new TypeReference<>() {}
        );

        assertThat(retrievedApiSubscriptions)
            .hasSize(2)
            .as("Retrieved subscriptions should match created subscriptions")
            .first()
            .extracting(ApiSubscription::getListType, ApiSubscription::getSensitivity)
            .containsExactly(ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC);
    }

    @Test
    void testGetThirdPartySubscriptionNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_SUBSCRIPTION_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedGetThirdPartyConfigurationByUserId() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(THIRD_PARTY_SUBSCRIPTION_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mvc.perform(request)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORISED_USERNAME, authorities = {UNAUTHORISED_ROLE})
    void testUnauthorisedUpdateThirdPartyConfiguration() throws Exception {
        when(thirdPartyAuthorisationService.userCanManageThirdParty(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(THIRD_PARTY_SUBSCRIPTION_PATH + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(apiSubscription1)));

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

    private MvcResult createApiSubscriptions() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(THIRD_PARTY_SUBSCRIPTION_PATH)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(apiSubscription1)));

        return mvc.perform(request)
            .andExpect(status().isCreated())
            .andReturn();
    }
}
