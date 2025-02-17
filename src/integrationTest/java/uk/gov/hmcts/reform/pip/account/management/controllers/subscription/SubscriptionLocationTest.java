package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;
import uk.gov.hmcts.reform.pip.model.system.admin.ActionResult;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubscriptionLocationTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String SUBSCRIPTION_PATH = "/subscription";
    private static final String SUBSCRIPTIONS_BY_LOCATION = "/subscription/location/";

    private static final String LOCATION_ID = "9";
    private static final String LOCATION_NAME = "Single Justice Procedure";
    private static final UUID USER_ID = UUID.fromString("87f907d2-eb28-42cc-b6e1-ae2b03f7bba7");
    private static final String ACTIONING_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba5";

    private static final String VALIDATION_EMPTY_RESPONSE = "Returned response is empty";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String NOT_FOUND_STATUS_CODE = "Status code does not match not found";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String SYSTEM_ADMIN_PROVENANCE_ID = "e5f1cc77-6e9a-40ab-8da0-a9666b328466";
    private static final String SYSTEM_ADMIN_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba4";
    private static final String USER_ID_HEADER = "x-user-id";
    private static final String TEST_EMAIL = "test-email-cath@justice.gov.uk";

    private static final Subscription SUBSCRIPTION = new Subscription();

    @Autowired
    protected MockMvc mvc;

    @BeforeAll
    void setup() {
        OBJECT_MAPPER.findAndRegisterModules();
        SUBSCRIPTION.setChannel(Channel.API_COURTEL);
        SUBSCRIPTION.setSearchType(SearchType.LOCATION_ID);
        SUBSCRIPTION.setUserId(USER_ID);
        SUBSCRIPTION.setSearchType(SearchType.LOCATION_ID);
        SUBSCRIPTION.setSearchValue(LOCATION_ID);
        SUBSCRIPTION.setLocationName(LOCATION_NAME);
        SUBSCRIPTION.setCreatedDate(LocalDateTime.now());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-verified-users.sql")
    void testFindSubscriptionsByLocationId() throws Exception {
        MockHttpServletRequestBuilder createSubscriptionRequest = MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(SUBSCRIPTION))
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createSubscriptionRequest)
            .andExpect(status().isCreated());

        MvcResult response = mvc.perform(get(SUBSCRIPTIONS_BY_LOCATION + LOCATION_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        List<Subscription> userSubscriptions =
            List.of(OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), Subscription[].class));

        assertEquals(1, userSubscriptions.size(),
                     "Subscriptions list for location id " + LOCATION_ID + " not found");
        assertEquals(LOCATION_ID, userSubscriptions.get(0).getSearchValue(),
                     "Subscriptions list for location id " + LOCATION_ID + " not found");
    }

    @Test
    void testFindSubscriptionsByLocationIdNotFound() throws Exception {
        MvcResult response = mvc.perform(get(SUBSCRIPTIONS_BY_LOCATION + LOCATION_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertEquals(NOT_FOUND.value(), response.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testFindSubscriptionsByLocationIdUnauthorized() throws Exception {
        MvcResult response = mvc.perform(get(SUBSCRIPTIONS_BY_LOCATION + LOCATION_ID))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(FORBIDDEN.value(), response.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
        scripts = { "classpath:add-admin-users.sql", "classpath:add-verified-users.sql"})
    void testDeleteSubscriptionByLocation() throws Exception {
        doNothing().when(publicationService)
            .sendLocationDeletionSubscriptionEmail(List.of(TEST_EMAIL), LOCATION_NAME);

        doNothing().when(publicationService).sendSystemAdminEmail(
            List.of(TEST_EMAIL, TEST_EMAIL), TEST_EMAIL, ActionResult.SUCCEEDED, SYSTEM_ADMIN_PROVENANCE_ID
        );

        MockHttpServletRequestBuilder createSubscriptionRequest = MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(SUBSCRIPTION))
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mvc.perform(createSubscriptionRequest)
            .andExpect(status().isCreated());

        mvc.perform(get(SUBSCRIPTIONS_BY_LOCATION + LOCATION_ID))
            .andExpect(status().isOk());

        MvcResult deleteResponse = mvc.perform(delete(
                SUBSCRIPTIONS_BY_LOCATION + LOCATION_ID)
                                                   .header(USER_ID_HEADER, SYSTEM_ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(deleteResponse.getResponse(), VALIDATION_EMPTY_RESPONSE);

        assertEquals(String.format("Total 1 subscriptions deleted for location id %s", LOCATION_ID),
                     deleteResponse.getResponse().getContentAsString(), "Responses are not equal");
    }

    @Test
    void testDeleteSubscriptionByLocationNotFound() throws Exception {
        MvcResult response = mvc.perform(delete(
                SUBSCRIPTIONS_BY_LOCATION + LOCATION_ID)
                                             .header(USER_ID_HEADER, SYSTEM_ADMIN_USER_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertEquals(NOT_FOUND.value(), response.getResponse().getStatus(), NOT_FOUND_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testDeleteSubscriptionByLocationUnauthorized() throws Exception {
        MvcResult response = mvc.perform(delete(SUBSCRIPTIONS_BY_LOCATION + LOCATION_ID)
                                             .header(USER_ID_HEADER, SYSTEM_ADMIN_USER_ID))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(FORBIDDEN.value(), response.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
    }
}
