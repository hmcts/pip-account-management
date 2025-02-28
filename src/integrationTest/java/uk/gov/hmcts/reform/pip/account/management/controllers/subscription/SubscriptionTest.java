package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.io.IOUtils;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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
import uk.gov.hmcts.reform.pip.account.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.CaseSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.LocationSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.UserSubscription;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@SuppressWarnings({"PMD.ExcessiveImports"})
class SubscriptionTest extends IntegrationTestBase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String LOCATION_NAME = "Single Justice Procedure";
    private static final String UUID_STRING = "f54c9783-7f56-4a69-91bc-55b582c0206f";
    private static final String VALID_USER_ID = "60e75e34-ad8e-4ac3-8f26-7de73e5c987b";

    private static final String VALIDATION_EMPTY_RESPONSE = "Returned response is empty";
    private static final String VALIDATION_CHANNEL_NAME = "Returned subscription channel "
        + "does not match expected channel";
    private static final String VALIDATION_SEARCH_TYPE = "Returned search type does not match expected type";
    private static final String VALIDATION_SEARCH_VALUE = "Returned search value does not match expected value";
    private static final String VALIDATION_USER_ID = "Returned user ID does not match expected user ID";
    private static final String VALIDATION_CASE_NAME = "Returned case name does not match expected case name";
    private static final String VALIDATION_CASE_NUMBER = "Returned case number does not match expected case number";
    private static final String VALIDATION_CASE_URN = "Returned URN does not match expected URN";
    private static final String VALIDATION_PARTY_NAMES = "Returned party names do not match expected parties";
    private static final String VALIDATION_LOCATION_NAME =
        "Returned location name does not match expected location name";
    private static final String VALIDATION_BAD_REQUEST = "Incorrect response - should be 400.";
    private static final String VALIDATION_CASE_ID = "Case ID does not match expected case";
    private static final String VALIDATION_LOCATION_LIST = "Location subscription list contains unknown locations";
    private static final String VALIDATION_SUBSCRIPTION_LIST = "The expected subscription is not displayed";
    private static final String VALIDATION_NO_SUBSCRIPTIONS = "User has unknown subscriptions";
    private static final String VALIDATION_ONE_CASE_LOCATION = "Location subscription list does not contain 1 case";
    private static final String VALIDATION_DATE_ADDED = "Date added does not match the expected date added";
    private static final String VALIDATION_UUID = "UUID should not be null";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String NOT_FOUND_STATUS_CODE = "Status code does not match not found";
    private static final String RESPONSE_MATCH = "Response should match";
    private static final String SUBSCRIBER_REQUEST_SUCCESS = "Subscriber request has been accepted";

    private static final String RAW_JSON_MISSING_SEARCH_VALUE =
        "{\"userId\": \"3\", \"searchType\": \"CASE_ID\",\"channel\": \"EMAIL\"}";
    private static final String RAW_JSON_MISSING_SEARCH_TYPE =
        "{\"userId\": \"3\", \"searchType\": \"123\",\"channel\": \"EMAIL\"}";
    private static final String RAW_JSON_MISSING_CHANNEL =
        "{\"userId\": \"3\", \"searchType\": \"CASE_ID\",\"searchValue\": \"321\"}";

    private static final String LOCATION_ID = "9";
    private static final String CASE_ID = "T485913";
    private static final String CASE_URN = "IBRANE1BVW";
    private static final String CASE_NAME = "Tom Clancy";
    private static final String PARTY_NAMES = "Party A, Party B";

    private static final String SUBSCRIPTION_BASE_URL = "/subscription/";
    private static final String SUBSCRIPTION_PATH = "/subscription";
    private static final String MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL_V2 = "/subscription/v2/mi-data-all";
    private static final String MI_REPORTING_SUBSCRIPTION_DATA_LOCATION_URL_V2 = "/subscription/v2/mi-data-location";
    private static final String SUBSCRIPTION_USER_PATH = "/subscription/user/" + UUID_STRING;
    private static final String ARTEFACT_RECIPIENT_PATH = "/subscription/artefact-recipients";
    private static final String DELETED_ARTEFACT_RECIPIENT_PATH = "/subscription/deleted-artefact";
    private static final String DELETED_BULK_SUBSCRIPTION_V2_PATH = "/subscription/bulk";

    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String OPENING_BRACKET = "[\"";
    private static final String CLOSING_BRACKET = "\"]";
    private static final String DOUBLE_QUOTE_COMMA = "\",\"";

    private static final String ACTIONING_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba5";
    private static final String INVALID_ACTIONING_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba6";
    private static final String SYSTEM_ADMIN_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba4";
    private static final String USER_ID_HEADER = "x-user-id";
    private static final String TEST_EMAIL = "test-email-cath@justice.gov.uk";

    private static final String ADD_ADMIN_USERS_SCRIPT = "classpath:add-admin-users.sql";
    private static final String ADD_VERIFIED_USERS_SCRIPT = "classpath:add-verified-users.sql";
    private static final Subscription SUBSCRIPTION = new Subscription();

    private static PiUser systemAdminUser = new PiUser();
    private static PiUser verifiedUser = new PiUser();

    private static String rawArtefact;

    @Autowired
    private MockMvc mvc;

    @BeforeAll
    static void setup() throws IOException {
        OBJECT_MAPPER.findAndRegisterModules();
        SUBSCRIPTION.setChannel(Channel.API_COURTEL);
        SUBSCRIPTION.setSearchType(SearchType.LOCATION_ID);
        SUBSCRIPTION.setUserId(UUID_STRING);

        systemAdminUser.setRoles(SYSTEM_ADMIN);
        systemAdminUser.setEmail(TEST_EMAIL);
        verifiedUser.setRoles(Roles.VERIFIED);

        try (InputStream is = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("mock/artefact.json")) {
            rawArtefact = new String(IOUtils.toByteArray(Objects.requireNonNull(is)));
        }
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue) throws JsonProcessingException {
        SUBSCRIPTION.setSearchValue(searchValue);
        SUBSCRIPTION.setLocationName(LOCATION_NAME);
        SUBSCRIPTION.setCaseName(CASE_NAME);
        SUBSCRIPTION.setCaseNumber(CASE_ID);
        SUBSCRIPTION.setUrn(CASE_URN);
        SUBSCRIPTION.setChannel(Channel.EMAIL);
        SUBSCRIPTION.setCreatedDate(DATE_ADDED);

        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(SUBSCRIPTION))
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue, SearchType searchType,
                                                                  String userId)
        throws JsonProcessingException {

        SUBSCRIPTION.setUserId(userId);
        SUBSCRIPTION.setSearchType(searchType);
        return setupMockSubscription(searchValue);
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue, SearchType searchType,
                                                                  String userId, String caseNumber, String caseUrn)
        throws JsonProcessingException {

        SUBSCRIPTION.setUserId(userId);
        SUBSCRIPTION.setSearchType(searchType);
        SUBSCRIPTION.setCaseNumber(caseNumber);
        SUBSCRIPTION.setUrn(caseUrn);
        return setupMockSubscription(searchValue);

    }

    protected MockHttpServletRequestBuilder setupMockSubscriptionWithListType(String searchValue,
                                                                              SearchType searchType,
                                                                              String userId)
        throws JsonProcessingException {

        SUBSCRIPTION.setUserId(userId);
        SUBSCRIPTION.setSearchType(searchType);
        return setupMockSubscription(searchValue);
    }

    protected MockHttpServletRequestBuilder getSubscriptionByUuid(String searchValue) {
        return get(SUBSCRIPTION_PATH + '/' + searchValue);
    }

    protected MockHttpServletRequestBuilder setupRawJsonSubscription(String json) {
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(json)
            .header(USER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);
    }

    @DisplayName("Post a new subscription and then get it from db.")
    @Test
    void postEndpoint() throws Exception {
        SUBSCRIPTION.setPartyNames(PARTY_NAMES);
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );
        assertEquals(SUBSCRIPTION.getChannel(), returnedSubscription.getChannel(), VALIDATION_CHANNEL_NAME);
        assertEquals(SUBSCRIPTION.getSearchType(), returnedSubscription.getSearchType(), VALIDATION_SEARCH_TYPE);
        assertEquals(SUBSCRIPTION.getSearchValue(), returnedSubscription.getSearchValue(), VALIDATION_SEARCH_VALUE);
        assertEquals(SUBSCRIPTION.getUserId(), returnedSubscription.getUserId(), VALIDATION_USER_ID);

        assertNotNull(returnedSubscription.getId(), VALIDATION_UUID);

        assertEquals(CASE_NAME, returnedSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(CASE_ID, returnedSubscription.getCaseNumber(), VALIDATION_CASE_NUMBER);
        assertEquals(CASE_URN, returnedSubscription.getUrn(), VALIDATION_CASE_URN);
        assertEquals(PARTY_NAMES, returnedSubscription.getPartyNames(), VALIDATION_PARTY_NAMES);
        assertEquals(LOCATION_NAME, returnedSubscription.getLocationName(), VALIDATION_LOCATION_NAME);
    }

    @DisplayName("Ensure post endpoint actually posts a subscription to db")
    @Test
    void checkPostToDb() throws Exception {
        SUBSCRIPTION.setPartyNames(PARTY_NAMES);
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

        MvcResult response = mvc.perform(mappedSubscription)
            .andExpect(status().isCreated())
            .andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );
        MvcResult findResponse = mvc.perform(get(String.format(
            "/subscription/%s",
            returnedSubscription.getId()
        ))).andExpect(status().isOk()).andReturn();
        assertNotNull(findResponse.getResponse(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse2 = findResponse.getResponse().getContentAsString();
        Subscription returnedSubscription2 = OBJECT_MAPPER.readValue(subscriptionResponse2, Subscription.class);

        assertEquals(SUBSCRIPTION.getChannel(), returnedSubscription2.getChannel(), VALIDATION_CHANNEL_NAME);
        assertEquals(SUBSCRIPTION.getSearchType(), returnedSubscription2.getSearchType(), VALIDATION_SEARCH_TYPE);
        assertEquals(SUBSCRIPTION.getSearchValue(), returnedSubscription2.getSearchValue(), VALIDATION_SEARCH_VALUE);
        assertEquals(SUBSCRIPTION.getUserId(), returnedSubscription2.getUserId(), VALIDATION_USER_ID);
        assertNotNull(returnedSubscription2.getId(), VALIDATION_UUID);

        assertEquals(CASE_NAME, returnedSubscription2.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(CASE_ID, returnedSubscription2.getCaseNumber(), VALIDATION_CASE_NUMBER);
        assertEquals(CASE_URN, returnedSubscription2.getUrn(), VALIDATION_CASE_URN);
        assertEquals(PARTY_NAMES, returnedSubscription2.getPartyNames(), VALIDATION_PARTY_NAMES);
        assertEquals(LOCATION_NAME, returnedSubscription2.getLocationName(), VALIDATION_LOCATION_NAME);
    }

    @DisplayName("Checks for bad request for invalid searchType enum.")
    @Test
    void checkSearchTypeEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'searchType': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @DisplayName("Checks for bad request for invalid channel enum.")
    @Test
    void checkChannelEnum() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(
            "{'channel': 'INVALID_TYPE'}");
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);

    }

    @DisplayName("Checks for bad request when empty json is sent")
    @Test
    void checkEmptyPost() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription("{}");
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingSearchType() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_SEARCH_TYPE);
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingSearchValue() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_SEARCH_VALUE);
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    void checkMissingChannel() throws Exception {
        MockHttpServletRequestBuilder brokenSubscription = setupRawJsonSubscription(RAW_JSON_MISSING_CHANNEL);
        MvcResult response = mvc.perform(brokenSubscription)
            .andExpect(status().isBadRequest()).andReturn();
        assertEquals(400, response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_ADMIN_USERS_SCRIPT)
    void testDeleteSubscriptionByIdReturnsOkIfSystemAdmin() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                                 ACTIONING_USER_ID
        );

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );

        MvcResult deleteResponse = mvc.perform(delete(SUBSCRIPTION_BASE_URL + returnedSubscription.getId())
                                                   .header(USER_ID_HEADER, SYSTEM_ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(deleteResponse.getResponse(), VALIDATION_EMPTY_RESPONSE);
        assertEquals(
            String.format("Subscription: %s was deleted", returnedSubscription.getId()),
            deleteResponse.getResponse().getContentAsString(),
            RESPONSE_MATCH
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void testDeleteSubscriptionByIdReturnsOkIfUserMatched() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                                 ACTIONING_USER_ID
        );

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );

        MvcResult deleteResponse = mvc.perform(delete(SUBSCRIPTION_BASE_URL + returnedSubscription.getId())
                                                   .header(USER_ID_HEADER, ACTIONING_USER_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(deleteResponse.getResponse(), VALIDATION_EMPTY_RESPONSE);
        assertEquals(
            String.format("Subscription: %s was deleted", returnedSubscription.getId()),
            deleteResponse.getResponse().getContentAsString(),
            RESPONSE_MATCH
        );
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void testDeleteSubscriptionByIdReturnsForbiddenIfUserMismatched() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid = Arrays.stream(subscriptionResponse.split(" "))
            .max(Comparator.comparingInt(String::length))
            .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        Subscription returnedSubscription = OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );

        mvc.perform(delete(SUBSCRIPTION_BASE_URL + returnedSubscription.getId())
                        .header(USER_ID_HEADER, INVALID_ACTIONING_USER_ID))
            .andExpect(status().isForbidden());
    }

    @DisplayName("Check response if delete fails")
    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void failedDelete() throws Exception {
        MvcResult response = mvc.perform(delete(SUBSCRIPTION_BASE_URL + UUID_STRING)
                                             .header(USER_ID_HEADER, ACTIONING_USER_ID))
            .andExpect(status().isNotFound()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String errorResponse = response.getResponse().getContentAsString();
        ExceptionResponse exceptionResponse = OBJECT_MAPPER.readValue(errorResponse, ExceptionResponse.class);

        assertEquals(
            "No subscription found with the subscription id " + UUID_STRING,
            exceptionResponse.getMessage(),
            "Incorrect status code"
        );
    }

    @DisplayName("Check response if findBySubId fails")
    @Test
    void failedFind() throws Exception {
        MvcResult response = mvc.perform(get(SUBSCRIPTION_BASE_URL + UUID_STRING))
            .andExpect(status()
                           .isNotFound()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String errorResponse = response.getResponse().getContentAsString();
        ExceptionResponse exceptionResponse = OBJECT_MAPPER.readValue(errorResponse, ExceptionResponse.class);

        assertEquals(
            "No subscription found with the subscription id " + UUID_STRING,
            exceptionResponse.getMessage(),
            "Incorrect status code"
        );
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSuccessful() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, UUID_STRING));
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, UUID_STRING));
        mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(
            3,
            userSubscriptions.getLocationSubscriptions().size() + userSubscriptions
                .getCaseSubscriptions().size(),
            VALIDATION_SUBSCRIPTION_LIST
        );

        LocationSubscription location = userSubscriptions.getLocationSubscriptions().get(0);
        assertEquals(LOCATION_NAME, location.getLocationName(), VALIDATION_LOCATION_NAME);
        assertEquals(DATE_ADDED.withNano(0), location.getDateAdded().withNano(0),
                     VALIDATION_DATE_ADDED);

        CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().get(0);
        assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdWithParties() throws Exception {
        SUBSCRIPTION.setUserId(UUID_STRING);
        SUBSCRIPTION.setSearchType(SearchType.CASE_ID);
        SUBSCRIPTION.setPartyNames(PARTY_NAMES);
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(CASE_ID);

        mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(
            1,
            userSubscriptions.getLocationSubscriptions().size() + userSubscriptions
                .getCaseSubscriptions().size(),
            VALIDATION_SUBSCRIPTION_LIST
        );

        CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().get(0);
        assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
        assertEquals(PARTY_NAMES, caseSubscription.getPartyNames(), VALIDATION_PARTY_NAMES);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleLocation() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(1, userSubscriptions.getLocationSubscriptions().size(),
                     "Court subscription list does not contain 1 court");

        assertEquals(0, userSubscriptions.getCaseSubscriptions().size(),
                     "Court subscription list contains unknown cases");

        LocationSubscription location = userSubscriptions.getLocationSubscriptions().get(0);
        assertEquals(LOCATION_NAME, location.getLocationName(), VALIDATION_LOCATION_NAME);
        assertEquals(DATE_ADDED.withNano(0), location.getDateAdded().withNano(0),
                     VALIDATION_DATE_ADDED);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdSingleCaseId() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(0, userSubscriptions.getLocationSubscriptions().size(), VALIDATION_LOCATION_LIST);
        assertEquals(1, userSubscriptions.getCaseSubscriptions().size(), VALIDATION_ONE_CASE_LOCATION);

        CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().get(0);
        assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(SearchType.CASE_ID, caseSubscription.getSearchType(), VALIDATION_SEARCH_TYPE);
        assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testGetUsersSubscriptionsByUserIdSingleCaseUrn() throws Exception {
        mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN, UUID_STRING));

        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(0, userSubscriptions.getLocationSubscriptions().size(), VALIDATION_LOCATION_LIST);
        assertEquals(1, userSubscriptions.getCaseSubscriptions().size(), VALIDATION_ONE_CASE_LOCATION);

        CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().get(0);
        assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
        assertEquals(SearchType.CASE_URN, caseSubscription.getSearchType(), VALIDATION_SEARCH_TYPE);
        assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
        assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
    }

    @Test
    void testGetUsersSubscriptionsByUserIdNoSubscriptions() throws Exception {
        MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH))
            .andExpect(status().isOk())
            .andReturn();

        assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

        UserSubscription userSubscriptions =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);

        assertEquals(new UserSubscription(), userSubscriptions,
                     VALIDATION_NO_SUBSCRIPTIONS);
    }

    @Test
    void testBuildSubscriberListReturnsAccepted() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH);
    }

    @Test
    void testBuildSubscriberListCaseUrnNull() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID, CASE_ID, null));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH);
    }

    @Test
    void testBuildSubscriberListCaseNumberNull() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID, null, CASE_URN));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH);
    }

    @Test
    @WithMockUser(username = "unauthorized_find_by_id", authorities = {"APPROLE_unknown.find"})
    void testUnauthorizedBuildSubscriberList() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);

        MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    void testBuildCourtSubscribersListReturnsAccepted() throws Exception {
        mvc.perform(setupMockSubscriptionWithListType(LOCATION_ID, SearchType.LOCATION_ID,
                                                      VALID_USER_ID));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(),
                     RESPONSE_MATCH);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedCreateSubscription() throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

        MvcResult mvcResult =
            mvc.perform(mappedSubscription).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = "unauthorized_delete", authorities = {"APPROLE_unknown.delete"})
    void testUnauthorizedDeleteById() throws Exception {
        MvcResult mvcResult = mvc.perform(getSubscriptionByUuid(UUID.randomUUID().toString())
        ).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = "unauthorized_find_by_id", authorities = {"APPROLE_unknown.find"})
    void testUnauthorizedFindSubscriptionById() throws Exception {
        MvcResult mvcResult = mvc.perform(get(String.format("/subscription/%s", UUID.randomUUID())))
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = "unauthorized_find_by_user_id", authorities = {"APPROLE_unknown.find"})
    void testUnauthorizedFindByUserId() throws Exception {
        MvcResult mvcResult = mvc.perform(get(SUBSCRIPTION_USER_PATH)).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    void testBuildDeletedArtefactSubscribersReturnsAccepted() throws Exception {
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(DELETED_ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

        assertEquals("Deleted artefact third party subscriber notification request has been accepted",
                     result.getResponse().getContentAsString(), RESPONSE_MATCH);
    }


    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_ADMIN_USERS_SCRIPT)
    void testBulkDeletedSubscribersV2ReturnsOkIfSystemAdmin() throws Exception {
        MvcResult caseSubscription = mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, ACTIONING_USER_ID))
            .andReturn();
        MvcResult locationSubscription = mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                           ACTIONING_USER_ID
            ))
            .andReturn();

        String caseSubscriptionId = getSubscriptionId(caseSubscription.getResponse().getContentAsString());
        String locationSubscriptionId = getSubscriptionId(locationSubscription.getResponse().getContentAsString());

        String subscriptionIdRequest = OPENING_BRACKET + caseSubscriptionId + DOUBLE_QUOTE_COMMA
            + locationSubscriptionId + CLOSING_BRACKET;

        MvcResult deleteResponse = mvc.perform(delete(DELETED_BULK_SUBSCRIPTION_V2_PATH)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(subscriptionIdRequest)
                                                   .header(USER_ID_HEADER, SYSTEM_ADMIN_USER_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(String.format(
                         "Subscriptions with ID %s deleted",
                         caseSubscriptionId + ", " + locationSubscriptionId
                     ),
                     deleteResponse.getResponse().getContentAsString(), RESPONSE_MATCH
        );

        MvcResult getCaseSubscriptionResponse =
            mvc.perform(getSubscriptionByUuid(caseSubscriptionId))
                .andExpect(status().isNotFound()).andReturn();
        assertEquals(NOT_FOUND.value(), getCaseSubscriptionResponse.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE);

        MvcResult getLocationSubscriptionResponse =
            mvc.perform(getSubscriptionByUuid(locationSubscriptionId))
                .andExpect(status().isNotFound()).andReturn();
        assertEquals(NOT_FOUND.value(), getLocationSubscriptionResponse.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void testBulkDeletedSubscribersV2ReturnsOkIfUserMatched() throws Exception {
        MvcResult caseSubscription = mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, ACTIONING_USER_ID))
            .andReturn();
        MvcResult locationSubscription = mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                           ACTIONING_USER_ID
            ))
            .andReturn();

        String caseSubscriptionId = getSubscriptionId(caseSubscription.getResponse().getContentAsString());
        String locationSubscriptionId = getSubscriptionId(locationSubscription.getResponse().getContentAsString());

        String subscriptionIdRequest = OPENING_BRACKET + caseSubscriptionId + DOUBLE_QUOTE_COMMA
            + locationSubscriptionId + CLOSING_BRACKET;

        MvcResult deleteResponse = mvc.perform(delete(DELETED_BULK_SUBSCRIPTION_V2_PATH)
                                                   .contentType(MediaType.APPLICATION_JSON)
                                                   .content(subscriptionIdRequest)
                                                   .header(USER_ID_HEADER, ACTIONING_USER_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals(String.format(
                         "Subscriptions with ID %s deleted",
                         caseSubscriptionId + ", " + locationSubscriptionId
                     ),
                     deleteResponse.getResponse().getContentAsString(), RESPONSE_MATCH
        );

        MvcResult getCaseSubscriptionResponse =
            mvc.perform(getSubscriptionByUuid(caseSubscriptionId))
                .andExpect(status().isNotFound()).andReturn();
        assertEquals(NOT_FOUND.value(), getCaseSubscriptionResponse.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE);

        MvcResult getLocationSubscriptionResponse =
            mvc.perform(getSubscriptionByUuid(locationSubscriptionId))
                .andExpect(status().isNotFound()).andReturn();
        assertEquals(NOT_FOUND.value(), getLocationSubscriptionResponse.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE);
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = ADD_VERIFIED_USERS_SCRIPT)
    void testBulkDeletedSubscribersV2ReturnsForbiddenIfUserMismatched() throws Exception {
        MvcResult caseSubscription = mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID))
            .andReturn();
        MvcResult locationSubscription = mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                           ACTIONING_USER_ID
            ))
            .andReturn();

        String caseSubscriptionId = getSubscriptionId(caseSubscription.getResponse().getContentAsString());
        String locationSubscriptionId = getSubscriptionId(locationSubscription.getResponse().getContentAsString());

        String subscriptionIdRequest = OPENING_BRACKET + caseSubscriptionId + DOUBLE_QUOTE_COMMA
            + locationSubscriptionId + CLOSING_BRACKET;

        MvcResult response = mvc.perform(delete(DELETED_BULK_SUBSCRIPTION_V2_PATH)
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(subscriptionIdRequest)
                                             .header(USER_ID_HEADER, INVALID_ACTIONING_USER_ID))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(FORBIDDEN.value(), response.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    void testBulkDeletedSubscribersV2ReturnsNotFound() throws Exception {
        String subscriptionIdRequest = OPENING_BRACKET + UUID_STRING + CLOSING_BRACKET;

        MvcResult response = mvc.perform(delete(DELETED_BULK_SUBSCRIPTION_V2_PATH)
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(subscriptionIdRequest)
                                             .header(USER_ID_HEADER, ACTIONING_USER_ID))
            .andExpect(status().isNotFound()).andReturn();

        assertEquals(NOT_FOUND.value(), response.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE);
    }

    @Test
    void testBuildBulkDeletedSubscribersV2ReturnsNotFound() throws Exception {

        String subscriptionIdRequest = OPENING_BRACKET + UUID_STRING + CLOSING_BRACKET;

        MvcResult response = mvc.perform(delete(DELETED_BULK_SUBSCRIPTION_V2_PATH)
                                             .contentType(MediaType.APPLICATION_JSON)
                                             .content(subscriptionIdRequest)
                                             .header(USER_ID_HEADER, ACTIONING_USER_ID))
            .andExpect(status().isNotFound()).andReturn();

        assertEquals(NOT_FOUND.value(), response.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedBuildDeletedArtefactSubscribers() throws Exception {

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(DELETED_ARTEFACT_RECIPIENT_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .content(rawArtefact);
        MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    void testGetSubscriptionDataForMiReportingAllV2() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, VALID_USER_ID))
            .andExpect(status().isCreated());
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID))
            .andExpect(status().isCreated());

        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL_V2))
            .andExpect(status().isOk()).andReturn();

        List<AllSubscriptionMiData> allSubscriptionMiData =  Arrays.asList(
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), AllSubscriptionMiData[].class)
        );

        assertThat(allSubscriptionMiData)
            .as("Must contain the expected case subscription")
            .anyMatch(anySubscription -> anySubscription.getSearchType().equals(SearchType.CASE_ID)
                && anySubscription.getChannel().equals(Channel.EMAIL)
                && VALID_USER_ID.equals(anySubscription.getUserId())
            );

        assertThat(allSubscriptionMiData)
            .as("Must contain the expected location subscription")
            .anyMatch(locationSubscription -> locationSubscription.getSearchType().equals(SearchType.LOCATION_ID)
                && LOCATION_NAME.equals(locationSubscription.getLocationName())
                && locationSubscription.getChannel().equals(Channel.EMAIL)
                && VALID_USER_ID.equals(locationSubscription.getUserId())
            );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testGetSubscriptionDataForMiReportingAllV2Unauthorized() throws Exception {
        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL_V2))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(FORBIDDEN.value(), response.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testGetSubscriptionDataForMiReportingLocationV2() throws Exception {
        mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, VALID_USER_ID))
            .andExpect(status().isCreated());
        mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID))
            .andExpect(status().isCreated());

        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_LOCATION_URL_V2))
            .andExpect(status().isOk()).andReturn();

        List<LocationSubscriptionMiData> locationSubscriptions =  Arrays.asList(
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), LocationSubscriptionMiData[].class)
        );

        assertThat(locationSubscriptions).extracting(LocationSubscriptionMiData::getSearchValue)
            .as("Should not retrieve case subscriptions")
            .noneMatch(CASE_ID::equals);

        assertThat(locationSubscriptions)
            .as("Must contain the expected location subscription")
            .anyMatch(locationSubscription -> LOCATION_ID.equals(locationSubscription.getSearchValue())
                && LOCATION_NAME.equals(locationSubscription.getLocationName())
                && locationSubscription.getChannel().equals(Channel.EMAIL)
                && VALID_USER_ID.equals(locationSubscription.getUserId())
            );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testGetSubscriptionDataForMiReportingLocationV2Unauthorized() throws Exception {
        MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_LOCATION_URL_V2))
            .andExpect(status().isForbidden())
            .andReturn();

        assertEquals(FORBIDDEN.value(), response.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    private String getSubscriptionId(String response) {
        String subscriptionId;
        int startIndex = response.indexOf("the id ") + "the id ".length();
        int endIndex = response.indexOf(" for user ");
        subscriptionId = response.substring(startIndex, endIndex);
        return subscriptionId.trim();
    }
}
