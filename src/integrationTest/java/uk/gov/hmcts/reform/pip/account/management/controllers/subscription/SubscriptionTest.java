package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.io.IOUtils;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.SubscriptionAuthorisationService;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pip.model.account.Roles.SYSTEM_ADMIN;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@SuppressWarnings({"PMD.ExcessiveImports", "PMD.SignatureDeclareThrowsException"})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD,
    scripts = { "classpath:add-admin-users.sql", "classpath:add-verified-users.sql"})
class SubscriptionTest extends IntegrationTestBase {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String LOCATION_NAME = "Single Justice Procedure";
    private static final UUID UUID_STRING = UUID.fromString("87f907d2-eb28-42cc-b6e1-ae2b03f7bba5");
    private static final UUID VALID_USER_ID = UUID.fromString("60e75e34-ad8e-4ac3-8f26-7de73e5c987b");

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
    private static final String RAW_JSON_EMPTY_BODY = "{}";
    private static final String RAW_JSON_INVALID_CHANNEL = "{'channel': 'INVALID_TYPE'}";
    private static final String RAW_JSON_INVALID_SEARCH_TYPE = "{'searchType': 'INVALID_TYPE'}";

    private static final String LOCATION_ID = "9";
    private static final String CASE_ID = "T485913";
    private static final String CASE_URN = "IBRANE1BVW";
    private static final String CASE_NAME = "Tom Clancy";
    private static final String PARTY_NAMES = "Party A, Party B";

    private static final String SUBSCRIPTION_BASE_URL = "/subscription/";
    private static final String SUBSCRIPTION_PATH = "/subscription";
    private static final String MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL = "/subscription/mi-data-all";
    private static final String MI_REPORTING_SUBSCRIPTION_DATA_LOCATION_URL = "/subscription/mi-data-location";
    private static final String SUBSCRIPTION_USER_PATH = "/subscription/user/" + UUID_STRING;
    private static final String ARTEFACT_RECIPIENT_PATH = "/subscription/artefact-recipients";
    private static final String DELETED_ARTEFACT_RECIPIENT_PATH = "/subscription/deleted-artefact";
    private static final String DELETE_BULK_SUBSCRIPTION_PATH = "/subscription/bulk";

    private static final LocalDateTime DATE_ADDED = LocalDateTime.now();
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String OPENING_BRACKET = "[\"";
    private static final String CLOSING_BRACKET = "\"]";
    private static final String DOUBLE_QUOTE_COMMA = "\",\"";

    private static final String ACTIONING_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba5";
    private static final String INVALID_ACTIONING_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba6";
    private static final String SYSTEM_ADMIN_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba4";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final String TEST_EMAIL = "test-email-cath@justice.gov.uk";
    private static final Subscription SUBSCRIPTION = new Subscription();

    private static PiUser systemAdminUser = new PiUser();
    private static PiUser verifiedUser = new PiUser();

    private static String rawArtefact;

    @MockitoBean
    private SubscriptionAuthorisationService subscriptionAuthorisationService;

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

    @BeforeEach
    public void setupEach() {
        when(subscriptionAuthorisationService.userCanAddSubscriptions(any(), any())).thenReturn(true);
        when(subscriptionAuthorisationService.userCanDeleteSubscriptions(any(), any())).thenReturn(true);
        when(subscriptionAuthorisationService.userCanViewSubscriptions(any(), any())).thenReturn(true);
        when(subscriptionAuthorisationService.userCanBulkDeleteSubscriptions(any(), any())).thenReturn(true);
        when(subscriptionAuthorisationService.userCanUpdateSubscriptions(any(), any())).thenReturn(true);
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
            .header(REQUESTER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String searchValue, SearchType searchType,
                                                                  UUID userId)
        throws JsonProcessingException {
        SUBSCRIPTION.setUserId(userId);
        SUBSCRIPTION.setSearchType(searchType);
        return setupMockSubscription(searchValue);
    }

    protected MockHttpServletRequestBuilder setupMockSubscription(String caseNumber, String caseUrn)
        throws JsonProcessingException {
        SUBSCRIPTION.setUserId(VALID_USER_ID);
        SUBSCRIPTION.setSearchType(SearchType.CASE_ID);
        SUBSCRIPTION.setCaseNumber(caseNumber);
        SUBSCRIPTION.setUrn(caseUrn);
        return setupMockSubscription(CASE_ID);

    }

    protected MockHttpServletRequestBuilder setupMockSubscriptionWithListType()

        throws JsonProcessingException {
        SUBSCRIPTION.setUserId(VALID_USER_ID);
        SUBSCRIPTION.setSearchType(SearchType.LOCATION_ID);
        return setupMockSubscription(LOCATION_ID);
    }


    protected MockHttpServletRequestBuilder getSubscriptionByUuid(String searchValue) {
        return get(SUBSCRIPTION_PATH + '/' + searchValue);
    }

    protected MockHttpServletRequestBuilder setupRawJsonSubscription(String json) {
        return MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(json)
            .header(REQUESTER_ID_HEADER, ACTIONING_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);
    }

    @Nested
    class CreateSubscription {

        @Test
        void checkCreateSubscription() throws Exception {
            SUBSCRIPTION.setPartyNames(PARTY_NAMES);
            MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(
                LOCATION_ID, SearchType.LOCATION_ID, UUID.fromString(ACTIONING_USER_ID));

            MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
            assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

            String subscriptionResponse = response.getResponse().getContentAsString();
            assertTrue(subscriptionResponse.startsWith("Subscription created with the id"),
                         "Created subscription response does not match expected");

            assertTrue(subscriptionResponse.endsWith("for user " + ACTIONING_USER_ID),
                       "Created subscription response does not match expected");
        }

        @Test
        void checkErrorResponseIsReturnedWhenUserDoesNotExist() throws Exception {
            SUBSCRIPTION.setUserId(UUID.randomUUID());
            MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

            MvcResult mvcResult = mvc.perform(mappedSubscription).andExpect(status().isNotFound()).andReturn();
            assertTrue(mvcResult.getResponse().getContentAsString().contains(
                "No user found with the userId: " + SUBSCRIPTION.getUserId()), "Error message is incorrect");
        }

        @ParameterizedTest
        @ValueSource(strings = {RAW_JSON_EMPTY_BODY, RAW_JSON_MISSING_SEARCH_TYPE,
            RAW_JSON_MISSING_SEARCH_VALUE, RAW_JSON_MISSING_CHANNEL,
            RAW_JSON_INVALID_CHANNEL, RAW_JSON_INVALID_SEARCH_TYPE})
        void checkMissingFields(String subscriptionInput) throws Exception {
            MockHttpServletRequestBuilder request = setupRawJsonSubscription(subscriptionInput);
            assertRequestResponseStatus(mvc, request, 400);
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedCreateSubscription() throws Exception {
            when(subscriptionAuthorisationService.userCanAddSubscriptions(any(), any())).thenReturn(false);
            MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID);

            assertRequestResponseStatus(mvc, get(String.format("/subscription/%s", UUID.randomUUID())),
                                        FORBIDDEN.value());
            assertRequestResponseStatus(mvc, mappedSubscription, FORBIDDEN.value());
        }
    }

    @Nested
    class DeleteSubscriptionById {

        @ParameterizedTest
        @ValueSource(strings = {SYSTEM_ADMIN_USER_ID, ACTIONING_USER_ID})
        @DisplayName("This covers both System Admin and Verified deleting their own subscription")
        void testDeleteSubscriptionById(String userId) throws Exception {
            Subscription returnedSubscription = createdAndExtractSubscription(ACTIONING_USER_ID);

            MvcResult deleteResponse = mvc.perform(delete(SUBSCRIPTION_BASE_URL + returnedSubscription.getId())
                                                       .header(REQUESTER_ID_HEADER, userId))
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
        @SuppressWarnings({"PMD.UnitTestShouldIncludeAssert"})
        void testDeleteSubscriptionByIdReturnsForbiddenIfUserMismatched() throws Exception {
            Subscription returnedSubscription = createdAndExtractSubscription(UUID_STRING.toString());

            when(subscriptionAuthorisationService.userCanDeleteSubscriptions(any(), any())).thenReturn(false);
            mvc.perform(delete(SUBSCRIPTION_BASE_URL + returnedSubscription.getId())
                            .header(REQUESTER_ID_HEADER, INVALID_ACTIONING_USER_ID))
                .andExpect(status().isForbidden());
        }

        @DisplayName("Check response if delete fails")
        @Test
        void failToDeleteSubscriptionWhenDoesNotExist() throws Exception {
            MvcResult response = mvc.perform(delete(SUBSCRIPTION_BASE_URL + UUID_STRING)
                                                 .header(REQUESTER_ID_HEADER, ACTIONING_USER_ID))
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

        @Test
        void testBadRequestIfHeaderNotProvidedForDeleteById() throws Exception {
            Subscription returnedSubscription = createdAndExtractSubscription(ACTIONING_USER_ID);

            assertRequestResponseStatus(mvc, delete(SUBSCRIPTION_BASE_URL + returnedSubscription.getId()),
                                        BAD_REQUEST.value());
        }

        @Test
        void testUnauthorizedDeleteById() throws Exception {
            Subscription returnedSubscription = createdAndExtractSubscription(ACTIONING_USER_ID);

            when(subscriptionAuthorisationService.userCanDeleteSubscriptions(any(), any())).thenReturn(false);
            MvcResult mvcResult = mvc.perform(delete(SUBSCRIPTION_BASE_URL + returnedSubscription.getId())
                                                  .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_USER_ID)
                                                  .with(user(UNAUTHORIZED_USERNAME).authorities(
                                                      new SimpleGrantedAuthority(UNAUTHORIZED_ROLE))))
                .andExpect(status().isForbidden()).andReturn();

            assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
        }

    }

    @Nested
    class BulkDeleteSubscriptions {

        @ParameterizedTest
        @ValueSource(strings = {SYSTEM_ADMIN_USER_ID, ACTIONING_USER_ID})
        void testBulkDeletedSubscribersReturnsOk(String userId) throws Exception {
            MvcResult caseSubscription = mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID,
                                                                           UUID.fromString(ACTIONING_USER_ID)))
                .andReturn();

            MvcResult locationSubscription = mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                               UUID.fromString(ACTIONING_USER_ID)
                ))
                .andReturn();

            String caseSubscriptionId = getSubscriptionId(caseSubscription.getResponse().getContentAsString());
            String locationSubscriptionId = getSubscriptionId(locationSubscription.getResponse().getContentAsString());

            String subscriptionIdRequest = OPENING_BRACKET + caseSubscriptionId + DOUBLE_QUOTE_COMMA
                + locationSubscriptionId + CLOSING_BRACKET;

            MvcResult deleteResponse = mvc.perform(delete(DELETE_BULK_SUBSCRIPTION_PATH)
                                                       .contentType(MediaType.APPLICATION_JSON)
                                                       .content(subscriptionIdRequest)
                                                       .header(REQUESTER_ID_HEADER, userId))
                .andExpect(status().isOk())
                .andReturn();

            assertEquals(String.format(
                             "Subscriptions with ID %s deleted",
                             caseSubscriptionId + ", " + locationSubscriptionId
                         ),
                         deleteResponse.getResponse().getContentAsString(), RESPONSE_MATCH
            );

            assertRequestResponseStatus(mvc, getSubscriptionByUuid(caseSubscriptionId), NOT_FOUND.value());
            assertRequestResponseStatus(mvc, getSubscriptionByUuid(locationSubscriptionId), NOT_FOUND.value());
        }

        @Test
        void testBulkDeletedSubscribersReturnsForbiddenIfUserMismatched() throws Exception {
            MvcResult caseSubscription = mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID))
                .andReturn();
            MvcResult locationSubscription = mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                               UUID.fromString(ACTIONING_USER_ID)
                ))
                .andReturn();

            String caseSubscriptionId = getSubscriptionId(caseSubscription.getResponse().getContentAsString());
            String locationSubscriptionId = getSubscriptionId(locationSubscription.getResponse().getContentAsString());

            String subscriptionIdRequest = OPENING_BRACKET + caseSubscriptionId + DOUBLE_QUOTE_COMMA
                + locationSubscriptionId + CLOSING_BRACKET;

            when(subscriptionAuthorisationService.userCanBulkDeleteSubscriptions(any(), any())).thenReturn(false);
            MvcResult response = mvc.perform(delete(DELETE_BULK_SUBSCRIPTION_PATH)
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .content(subscriptionIdRequest)
                                                 .header(REQUESTER_ID_HEADER, INVALID_ACTIONING_USER_ID))
                .andExpect(status().isForbidden())
                .andReturn();

            assertEquals(FORBIDDEN.value(), response.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
        }

        @Test
        void testBulkDeleteSubscriptionReturnsNotFound() throws Exception {
            String subscriptionIdRequest = OPENING_BRACKET + UUID_STRING + CLOSING_BRACKET;

            MvcResult response = mvc.perform(delete(DELETE_BULK_SUBSCRIPTION_PATH)
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .content(subscriptionIdRequest)
                                                 .header(REQUESTER_ID_HEADER, ACTIONING_USER_ID))
                .andExpect(status().isNotFound()).andReturn();

            assertEquals(NOT_FOUND.value(), response.getResponse().getStatus(), NOT_FOUND_STATUS_CODE);
        }

        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedBulkDeleteSubscription() throws Exception {
            String subscriptionIdRequest = OPENING_BRACKET + UUID_STRING + CLOSING_BRACKET;

            when(subscriptionAuthorisationService.userCanBulkDeleteSubscriptions(any(), any())).thenReturn(false);
            MvcResult response = mvc.perform(delete(DELETE_BULK_SUBSCRIPTION_PATH)
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .content(subscriptionIdRequest)
                                                 .header(REQUESTER_ID_HEADER, ACTIONING_USER_ID))
                .andExpect(status().isForbidden()).andReturn();

            assertEquals(FORBIDDEN.value(), response.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
        }

    }

    @Nested
    class FindBySubscriptionId {

        @Test
        void testFindSubscriptionByIdSuccessful() throws Exception {
            SUBSCRIPTION.setPartyNames(PARTY_NAMES);
            Subscription returnedSubscription = createdAndExtractSubscription(UUID_STRING.toString());

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

        @Test
        void failToFindSubscriptionWhenDoesNotExist() throws Exception {
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
        @WithMockUser(username = "unauthorized_find_by_id", authorities = {"APPROLE_unknown.find"})
        void testUnauthorizedFindSubscriptionById() throws Exception {
            assertRequestResponseStatus(mvc, get(String.format("/subscription/%s", UUID.randomUUID())),
                                        FORBIDDEN.value());
        }

    }

    @Nested
    class FindSubscriptionsByUserId {

        @Test
        void testGetUsersSubscriptionsByUserIdSuccessful() throws Exception {
            mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, UUID_STRING));
            mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, UUID_STRING));
            mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN, UUID_STRING));

            UserSubscription userSubscriptions = getUserSubscriptions();

            assertEquals(
                3,
                userSubscriptions.getLocationSubscriptions().size() + userSubscriptions
                    .getCaseSubscriptions().size(),
                VALIDATION_SUBSCRIPTION_LIST
            );

            LocationSubscription location = userSubscriptions.getLocationSubscriptions().getFirst();
            assertEquals(LOCATION_NAME, location.getLocationName(), VALIDATION_LOCATION_NAME);
            assertEquals(DATE_ADDED.withNano(0), location.getDateAdded().withNano(0),
                         VALIDATION_DATE_ADDED);

            CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().getFirst();
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

            UserSubscription userSubscriptions = getUserSubscriptions();

            assertEquals(
                1,
                userSubscriptions.getLocationSubscriptions().size() + userSubscriptions
                    .getCaseSubscriptions().size(),
                VALIDATION_SUBSCRIPTION_LIST
            );

            CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().getFirst();
            assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
            assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
            assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
            assertEquals(PARTY_NAMES, caseSubscription.getPartyNames(), VALIDATION_PARTY_NAMES);
        }

        @Test
        void testGetUsersSubscriptionsByUserIdSingleLocation() throws Exception {
            mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, UUID_STRING));

            UserSubscription userSubscriptions = getUserSubscriptions();

            assertEquals(1, userSubscriptions.getLocationSubscriptions().size(),
                         "Court subscription list does not contain 1 court");

            assertEquals(0, userSubscriptions.getCaseSubscriptions().size(),
                         "Court subscription list contains unknown cases");

            LocationSubscription location = userSubscriptions.getLocationSubscriptions().getFirst();
            assertEquals(LOCATION_NAME, location.getLocationName(), VALIDATION_LOCATION_NAME);
            assertEquals(DATE_ADDED.withNano(0), location.getDateAdded().withNano(0),
                         VALIDATION_DATE_ADDED);
        }

        @Test
        void testGetUsersSubscriptionsByUserIdSingleCaseId() throws Exception {
            mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, UUID_STRING));

            UserSubscription userSubscriptions = getUserSubscriptions();

            assertEquals(0, userSubscriptions.getLocationSubscriptions().size(), VALIDATION_LOCATION_LIST);
            assertEquals(1, userSubscriptions.getCaseSubscriptions().size(), VALIDATION_ONE_CASE_LOCATION);

            CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().getFirst();
            assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
            assertEquals(SearchType.CASE_ID, caseSubscription.getSearchType(), VALIDATION_SEARCH_TYPE);
            assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
            assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
        }

        @Test
        void testGetUsersSubscriptionsByUserIdSingleCaseUrn() throws Exception {
            mvc.perform(setupMockSubscription(CASE_URN, SearchType.CASE_URN, UUID_STRING));

            UserSubscription userSubscriptions = getUserSubscriptions();

            assertEquals(0, userSubscriptions.getLocationSubscriptions().size(), VALIDATION_LOCATION_LIST);
            assertEquals(1, userSubscriptions.getCaseSubscriptions().size(), VALIDATION_ONE_CASE_LOCATION);

            CaseSubscription caseSubscription = userSubscriptions.getCaseSubscriptions().getFirst();
            assertEquals(CASE_NAME, caseSubscription.getCaseName(), VALIDATION_CASE_NAME);
            assertEquals(SearchType.CASE_URN, caseSubscription.getSearchType(), VALIDATION_SEARCH_TYPE);
            assertEquals(CASE_ID, caseSubscription.getCaseNumber(), VALIDATION_CASE_ID);
            assertEquals(CASE_URN, caseSubscription.getUrn(), VALIDATION_CASE_URN);
        }

        @Test
        void testGetUsersSubscriptionsByUserIdNoSubscriptions() throws Exception {
            UserSubscription userSubscriptions = getUserSubscriptions();

            assertEquals(new UserSubscription(), userSubscriptions, VALIDATION_NO_SUBSCRIPTIONS);
        }

        @Test
        @WithMockUser(username = "unauthorized_find_by_user_id", authorities = {"APPROLE_unknown.find"})
        void testUnauthorizedFindByUserId() throws Exception {
            when(subscriptionAuthorisationService.userCanViewSubscriptions(any(), any())).thenReturn(false);
            assertRequestResponseStatus(mvc, get(SUBSCRIPTION_USER_PATH)
                .header(REQUESTER_ID_HEADER, ACTIONING_USER_ID), FORBIDDEN.value());
        }

        private UserSubscription getUserSubscriptions() throws Exception {
            MvcResult response = mvc.perform(get(SUBSCRIPTION_USER_PATH)
                                                 .header(REQUESTER_ID_HEADER, ACTIONING_USER_ID))
                .andExpect(status().isOk())
                .andReturn();

            assertNotNull(response.getResponse(), VALIDATION_EMPTY_RESPONSE);

            return OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), UserSubscription.class);
        }

    }

    @Nested
    class ArtefactRecipients {

        @Test
        void testBuildSubscriberListReturnsAccepted() throws Exception {
            mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
            assertAcceptedArtefactRecipientRequest();
        }

        @Test
        void testBuildSubscriberListCaseUrnNull() throws Exception {
            mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
            assertAcceptedArtefactRecipientRequest();
        }

        @Test
        void testBuildSubscriberListCaseNumberNull() throws Exception {
            mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID));
            assertAcceptedArtefactRecipientRequest();
        }

        @Test
        void testBuildLocationSubscribersListReturnsAccepted() throws Exception {
            mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, VALID_USER_ID));
            assertAcceptedArtefactRecipientRequest();
        }

        @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
        private void assertAcceptedArtefactRecipientRequest() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(ARTEFACT_RECIPIENT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawArtefact);
            MvcResult result = mvc.perform(request).andExpect(status().isAccepted()).andReturn();

            assertEquals(SUBSCRIBER_REQUEST_SUCCESS, result.getResponse().getContentAsString(), RESPONSE_MATCH);
        }

        @Test
        @WithMockUser(username = "unauthorized_find_by_id", authorities = {"APPROLE_unknown.find"})
        void testUnauthorizedBuildSubscriberList() throws Exception {
            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(ARTEFACT_RECIPIENT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawArtefact);

            assertRequestResponseStatus(mvc, request, FORBIDDEN.value());
        }

    }

    @Nested
    class BulkDeleteArtefactSubscribers {

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
        void testBadRequestIfHeaderNotProvidedForBulkDelete() throws Exception {
            String subscriptionIdRequest = OPENING_BRACKET + UUID_STRING + CLOSING_BRACKET;

            MvcResult response = mvc.perform(delete(DELETE_BULK_SUBSCRIPTION_PATH)
                                                 .contentType(MediaType.APPLICATION_JSON)
                                                 .content(subscriptionIdRequest))
                .andExpect(status().isBadRequest()).andReturn();

            assertEquals(BAD_REQUEST.value(), response.getResponse().getStatus(), VALIDATION_BAD_REQUEST);
        }


        @Test
        @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
        void testUnauthorizedBuildDeletedArtefactSubscribers() throws Exception {

            MockHttpServletRequestBuilder request = MockMvcRequestBuilders
                .post(DELETED_ARTEFACT_RECIPIENT_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(rawArtefact);
            MvcResult mvcResult = mvc.perform(request).andExpect(status().isForbidden()).andReturn();

            assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(), FORBIDDEN_STATUS_CODE);
        }

    }

    @Nested
    class MiReporting {

        @Test
        void testGetSubscriptionDataForMiReportingAll() throws Exception {
            mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, VALID_USER_ID))
                .andExpect(status().isCreated());
            mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID))
                .andExpect(status().isCreated());

            MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL))
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
        void testGetSubscriptionDataForMiReportingAllUnauthorized() throws Exception {
            assertRequestResponseStatus(mvc, get(MI_REPORTING_SUBSCRIPTION_DATA_ALL_URL), FORBIDDEN.value());
        }

        @Test
        void testGetSubscriptionDataForMiReportingLocation() throws Exception {
            mvc.perform(setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID, VALID_USER_ID))
                .andExpect(status().isCreated());
            mvc.perform(setupMockSubscription(CASE_ID, SearchType.CASE_ID, VALID_USER_ID))
                .andExpect(status().isCreated());

            MvcResult response = mvc.perform(get(MI_REPORTING_SUBSCRIPTION_DATA_LOCATION_URL))
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
        void testGetSubscriptionDataForMiReportingLocationUnauthorized() throws Exception {
            assertRequestResponseStatus(mvc, get(MI_REPORTING_SUBSCRIPTION_DATA_LOCATION_URL), FORBIDDEN.value());
        }
    }


    private String getSubscriptionId(String response) {
        String subscriptionId;
        int startIndex = response.indexOf("the id ") + "the id ".length();
        int endIndex = response.indexOf(" for user ");
        subscriptionId = response.substring(startIndex, endIndex);
        return subscriptionId.trim();
    }

    private Subscription createdAndExtractSubscription(String userId) throws Exception {
        MockHttpServletRequestBuilder mappedSubscription = setupMockSubscription(LOCATION_ID, SearchType.LOCATION_ID,
                                                                                 UUID.fromString(userId)
        );

        MvcResult response = mvc.perform(mappedSubscription).andExpect(status().isCreated()).andReturn();
        assertNotNull(response.getResponse().getContentAsString(), VALIDATION_EMPTY_RESPONSE);

        String subscriptionResponse = response.getResponse().getContentAsString();
        String ourUuid =
            Arrays.stream(subscriptionResponse.split(" ")).max(Comparator.comparingInt(String::length))
                .orElse(null);

        MvcResult getResponse = mvc.perform(getSubscriptionByUuid(ourUuid)).andReturn();
        return OBJECT_MAPPER.readValue(
            getResponse.getResponse().getContentAsString(),
            Subscription.class
        );

    }
}
