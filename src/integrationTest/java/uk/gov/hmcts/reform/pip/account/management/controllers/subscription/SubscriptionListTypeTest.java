package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
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
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
class SubscriptionListTypeTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String USER_ID_HEADER = "x-user-id";
    private static final String ACTIONING_USER_ID = "f54c9783-7f56-4a69-91bc-55b582c0206f";
    private static final UUID VALID_USER_ID = UUID.fromString("87f907d2-eb28-42cc-b6e1-ae2b03f7bba5");
    private static final String CASE_ID = "T485913";

    private static final String SUBSCRIPTION_PATH = "/subscription";
    private static final String UPDATE_LIST_TYPE_PATH = "/subscription/configure-list-types/" + VALID_USER_ID;
    private static final String ADD_LIST_TYPE_PATH = "/subscription/add-list-types/" + VALID_USER_ID;

    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String RESPONSE_MATCH = "Response should match";

    private static final String ADD_VERIFIED_USERS_SCRIPT = "classpath:add-verified-users.sql";

    private SubscriptionListType subscriptionListType;

    @Autowired
    protected MockMvc mvc;

    @BeforeAll
    static void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    public void setupEach() {
        subscriptionListType = new SubscriptionListType();
        subscriptionListType.setListType(List.of("FAMILY_DAILY_CAUSE_LIST"));
        subscriptionListType.setListLanguage(List.of("ENGLISH"));
        subscriptionListType.setUserId(VALID_USER_ID);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void testAddListTypesForSubscription() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ADD_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(subscriptionListType));
        MvcResult result = mvc.perform(request).andExpect(status().isCreated()).andReturn();

        assertEquals(String.format("Location list Type successfully added for user %s", VALID_USER_ID),
                     result.getResponse().getContentAsString(), RESPONSE_MATCH);
    }

    @Test
    void testAddListTypesForSubscriptionWhenUserDoesNotExist() throws Exception {
        subscriptionListType.setUserId(UUID.randomUUID());
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ADD_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(subscriptionListType));
        MvcResult mvcResult = mvc.perform(request).andExpect(status().isNotFound()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains(
            "No user found with the userId: " + subscriptionListType.getUserId()), "Error message is incorrect");
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedAddListTypesForSubscription() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ADD_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(subscriptionListType));
        MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void testConfigureListTypesForSubscription() throws Exception {
        Subscription subscription = new Subscription();
        subscription.setChannel(Channel.API_COURTEL);
        subscription.setUserId(VALID_USER_ID);
        subscription.setSearchType(SearchType.CASE_ID);
        subscription.setSearchValue(CASE_ID);
        subscription.setCaseNumber(CASE_ID);
        subscription.setCreatedDate(LocalDateTime.now());

        MockHttpServletRequestBuilder createSubscriptionRequest = MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(subscription))
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createSubscriptionRequest)
            .andExpect(status().isCreated());

        MockHttpServletRequestBuilder updateListTypeRequest = MockMvcRequestBuilders
            .put(UPDATE_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(subscriptionListType));

        MvcResult result = mvc.perform(updateListTypeRequest)
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(String.format("Location list Type successfully updated for user %s", VALID_USER_ID),
                     result.getResponse().getContentAsString(), RESPONSE_MATCH);
    }

    @Test
    void testConfigureListTypesForSubscriptionWhenUserDoesNotExist() throws Exception {
        subscriptionListType.setUserId(UUID.randomUUID());
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(UPDATE_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(subscriptionListType));
        MvcResult mvcResult = mvc.perform(request).andExpect(status().isNotFound()).andReturn();
        assertTrue(mvcResult.getResponse().getContentAsString().contains(
            "No user found with the userId: " + subscriptionListType.getUserId()), "Error message is incorrect");
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void testUnauthorizedConfigureListTypesForSubscription() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(UPDATE_LIST_TYPE_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(OBJECT_MAPPER.writeValueAsString(subscriptionListType));
        MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
    }
}
