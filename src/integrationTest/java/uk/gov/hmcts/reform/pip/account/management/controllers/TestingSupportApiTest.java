package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.graph.users.item.UserItemRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.model.account.AuditLog;
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.UserSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.AccountAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.AuditAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.MediaApplicationAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.SubscriptionAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.ThirdPartyAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.reform.pip.model.enums.AuditAction.PUBLICATION_UPLOAD;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class TestingSupportApiTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TESTING_SUPPORT_BASE_URL = "/testing-support/";
    private static final String TESTING_SUPPORT_ACCOUNT_URL = TESTING_SUPPORT_BASE_URL + "account/";
    private static final String TESTING_SUPPORT_APPLICATION_URL = TESTING_SUPPORT_BASE_URL + "application/";
    private static final String TESTING_SUPPORT_CREATE_ACCOUNT_URL = TESTING_SUPPORT_BASE_URL + "account";
    private static final String TESTING_SUPPORT_SUBSCRIPTION_URL = TESTING_SUPPORT_BASE_URL + "subscription/";
    private static final String TESTING_SUPPORT_THIRD_PARTY_URL = TESTING_SUPPORT_BASE_URL + "third-party/";
    private static final String TESTING_SUPPORT_AUDIT_URL = TESTING_SUPPORT_BASE_URL + "audit/";

    private static final String ACCOUNT_URL = "/account/";
    private static final String ACCOUNT_ADD_USER_URL = ACCOUNT_URL + "add/pi";
    private static final String APPLICATION_URL = "/application";
    private static final String THIRD_PARTY_URL = "/third-party";
    private static final String THIRD_PARTY_SUBSCRIPTION_URL = THIRD_PARTY_URL + "/subscription";
    private static final String THIRD_PARTY_CONFIGURATION_URL = THIRD_PARTY_URL + "/configuration";
    private static final String B2C_URL = "URL";

    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final String SYSTEM_ADMIN_USER_ID = "87f907d2-eb28-42cc-b6e1-ae2b03f7bba2";
    private static final UUID REQUESTER_ID = UUID.randomUUID();

    private static final String EMAIL_PREFIX = "TEST_789_";
    private static final String EMAIL = EMAIL_PREFIX + UUID.randomUUID().toString() + "@test.com";
    private static final String NAME_PREFIX = "TEST_123_";
    private static final String PASSWORD = UUID.randomUUID().toString();
    private static final String ID = "1234";

    private static final String PROVENANCE_USER_ID = UUID.randomUUID().toString();
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final Roles ROLE = Roles.VERIFIED;
    private static final String GIVEN_NAME = "Given Name";
    private static final String SURNAME = "Surname";

    private static final String FULL_NAME = "Test user";
    private static final String EMPLOYER = "Test employer";
    private static final MediaApplicationStatus PENDING_STATUS = MediaApplicationStatus.PENDING;

    private static final String DESTINATION_URL = "https://test.com";
    private static final String TOKEN_URL = "https://test.token.com";
    private static final String CLIENT_ID_KEY = "clientId";
    private static final String CLIENT_SECRET_KEY = "clientSecret";
    private static final String SCOPE_KEY = "scope";

    private static final String SUBSCRIPTION_PATH = "/subscription";
    private static final String SUBSCRIPTION_BY_USER_PATH = "/subscription/user/%s";
    private static final String LOCATION_NAME_PREFIX = "TEST_123_";
    private static final String LOCATION_NAME = "Court1";

    private static final UUID USER_ID = UUID.fromString("87f907d2-eb28-42cc-b6e1-ae2b03f7bba5");
    private static final String CASE_ID = "T485913";

    private static final String AUDIT_URL = "/audit";
    private static final AuditAction ACTION = PUBLICATION_UPLOAD;
    private static final String DETAILS = "Publication successfully uploaded";

    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    GraphServiceClient graphClient;

    @Mock
    private UsersRequestBuilder usersRequestBuilder;

    @Mock
    private UserItemRequestBuilder userItemRequestBuilder;

    @Mock
    private ClientConfiguration clientConfiguration;

    @MockitoBean
    private AccountAuthorisationService accountAuthorisationService;

    @MockitoBean
    private MediaApplicationAuthorisationService mediaApplicationAuthorisationService;

    @MockitoBean
    private ThirdPartyAuthorisationService thirdPartyAuthorisationService;

    @MockitoBean
    private AuditAuthorisationService auditAuthorisationService;

    @MockitoBean
    private SubscriptionAuthorisationService subscriptionAuthorisationService;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void beforeEachSetUp() {
        when(accountAuthorisationService.userCanCreateAzureAccount(any())).thenReturn(true);
        when(accountAuthorisationService.userCanGetAccountByUserId(any(), any())).thenReturn(true);
        when(auditAuthorisationService.userCanViewAuditLogs(any())).thenReturn(true);
        when(accountAuthorisationService.userCanCreateAccount(any(), any())).thenReturn(true);
        when(subscriptionAuthorisationService.userCanAddSubscriptions(any(), any())).thenReturn(true);
        when(subscriptionAuthorisationService.userCanViewSubscriptions(any(), any())).thenReturn(true);
        when(thirdPartyAuthorisationService.userCanManageThirdParty(any())).thenReturn(true);
    }

    @Test
    void testTestingSupportCreateAccount() throws Exception {

        //Azure mock setup
        User userToReturn = new User();
        userToReturn.setId(ID);
        userToReturn.setGivenName(GIVEN_NAME);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        //Create new test user account
        AzureAccount newAccount = createAccount(PASSWORD);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(TESTING_SUPPORT_CREATE_ACCOUNT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(newAccount))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult postResponse = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isCreated())
            .andReturn();

        PiUser createdAccount = OBJECT_MAPPER.readValue(postResponse.getResponse().getContentAsString(), PiUser.class);

        assertEquals(EMAIL, createdAccount.getEmail(), "Azure account creation error");

        //User mock setup
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(any())).thenReturn(userItemRequestBuilder);
        when(userItemRequestBuilder.get()).thenReturn(new User());

        //Check whether account is created in Pi user table
        MockHttpServletRequestBuilder getPiUserRequest = get(ACCOUNT_URL + createdAccount.getUserId())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        MvcResult responseGetUser = mockMvc.perform(getPiUserRequest).andExpect(status().isOk()).andReturn();

        PiUser returnedUser = OBJECT_MAPPER.readValue(
            responseGetUser.getResponse().getContentAsString(),
            PiUser.class
        );
        assertEquals(EMAIL, returnedUser.getEmail(), "Users should match");

        //Delete test user
        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_ACCOUNT_URL + EMAIL_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("User account delete response does not match")
            .isEqualTo("1 account(s) deleted with email starting with " + EMAIL_PREFIX);

        //Check whether the user deleted in Pi user table
        mockMvc.perform(get(ACCOUNT_URL + createdAccount.getUserId())
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void testBadRequestTestingSupportCreateAccount() throws Exception {

        User userToReturn = new User();
        userToReturn.setId(ID);
        userToReturn.setGivenName(GIVEN_NAME);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        AzureAccount newAccount = createAccount("");

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(TESTING_SUPPORT_CREATE_ACCOUNT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(newAccount))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedTestingSupportCreateAccount() throws Exception {

        AzureAccount newAccount = createAccount(PASSWORD);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(TESTING_SUPPORT_CREATE_ACCOUNT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(newAccount))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isForbidden());
    }


    @Test
    void testTestingSupportDeleteAccountsWithEmailPrefix() throws Exception {
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.byUserId(any())).thenReturn(userItemRequestBuilder);
        when(userItemRequestBuilder.get()).thenReturn(new User());

        PiUser user = createUser();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ACCOUNT_ADD_USER_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(user)))
            .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult postResponse = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isCreated())
            .andReturn();

        Map<CreationEnum, List<String>> mappedResponse = OBJECT_MAPPER.readValue(
            postResponse.getResponse().getContentAsString(), new TypeReference<>() {
            }
        );

        assertThat(mappedResponse.get(CreationEnum.CREATED_ACCOUNTS))
            .as("No account created")
            .hasSize(1);

        String userId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).getFirst();
        mockMvc.perform(get(ACCOUNT_URL + userId)
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isOk());

        when(accountAuthorisationService.userCanDeleteAccount(any(), any())).thenReturn(true);
        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_ACCOUNT_URL + EMAIL_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Media application delete response does not match")
            .isEqualTo("1 account(s) deleted with email starting with " + EMAIL_PREFIX);

        when(accountAuthorisationService.userCanGetAccountByUserId(any(), any())).thenReturn(true);
        mockMvc.perform(get(ACCOUNT_URL + userId)
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void testTestingSupportDeleteApplicationsWithEmailPrefix() throws Exception {
        MediaApplication application = createApplication();

        when(mediaApplicationAuthorisationService.userCanViewMediaApplications(any())).thenReturn(true);
        mockMvc.perform(get(APPLICATION_URL + "/" + application.getId())
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isOk());

        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_APPLICATION_URL + EMAIL_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Media application delete response does not match")
            .isEqualTo("1 media application(s) deleted with email starting with " + EMAIL_PREFIX);

        when(mediaApplicationAuthorisationService.userCanViewMediaApplications(any())).thenReturn(true);
        mockMvc.perform(get(APPLICATION_URL + "/" + application.getId())
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    @Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:add-verified-users.sql")
    void testTestingSupportDeleteSubscriptionsWithLocationNamePrefix() throws Exception {
        Subscription subscription = createSubscription();

        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders.post(SUBSCRIPTION_PATH)
            .content(OBJECT_MAPPER.writeValueAsString(subscription))
            .header(REQUESTER_ID_HEADER, USER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(postRequest)
            .andExpect(status().isCreated());

        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Subscription response does not match")
            .isEqualTo("1 subscription(s) deleted for location name starting with " + LOCATION_NAME_PREFIX);

        MvcResult mvcResult = mockMvc.perform(get(String.format(SUBSCRIPTION_BY_USER_PATH,  USER_ID))
                                                  .header(REQUESTER_ID_HEADER, USER_ID)).andReturn();
        UserSubscription userSubscription =
            OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), UserSubscription.class);

        assertThat(userSubscription.getLocationSubscriptions().size()).isEqualTo(0);
        assertThat(userSubscription.getCaseSubscriptions().size()).isEqualTo(0);
        assertThat(userSubscription.getListTypeSubscriptions().size()).isEqualTo(0);
    }

    @Test
    void testTestingSupportDeleteThirdPartyUsersWithNamePrefix() throws Exception {
        ApiUser apiUser = creatThirdPartyUser();
        MockHttpServletRequestBuilder createUserRequest = MockMvcRequestBuilders
            .post(THIRD_PARTY_URL)
            .content(OBJECT_MAPPER.writeValueAsString(apiUser))
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult createUserResponse = mockMvc.perform(createUserRequest)
            .andExpect(status().isCreated())
            .andReturn();

        UUID userId = OBJECT_MAPPER.readValue(createUserResponse.getResponse().getContentAsString(), UUID.class);

        ApiSubscription apiSubscription = creatThirdPartySubscription(userId);
        MockHttpServletRequestBuilder createSubscriptionRequest = MockMvcRequestBuilders
            .post(THIRD_PARTY_SUBSCRIPTION_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(apiSubscription)))
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(createSubscriptionRequest)
            .andExpect(status().isCreated());

        ApiOauthConfiguration apiOauthConfiguration = creatThirdPartyOauthConfiguration(userId);
        MockHttpServletRequestBuilder createConfigurationRequest = MockMvcRequestBuilders
            .post(THIRD_PARTY_CONFIGURATION_URL)
            .content(OBJECT_MAPPER.writeValueAsString(apiOauthConfiguration))
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(createConfigurationRequest)
            .andExpect(status().isCreated());

        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_THIRD_PARTY_URL + NAME_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Third-party delete response does not match")
            .isEqualTo("1 third-party user(s) with name starting with " + NAME_PREFIX
                           + " and associated subscriptions/configurations deleted");

        mockMvc.perform(get(THIRD_PARTY_URL + "/" + userId)
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isNotFound());

        mockMvc.perform(get(THIRD_PARTY_SUBSCRIPTION_URL + "/" + userId)
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isNotFound());

        mockMvc.perform(get(THIRD_PARTY_CONFIGURATION_URL + "/" + userId)
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void testTestingSupportDeleteAuditLogsWithEmailPrefix() throws Exception {
        AuditLog auditLog = createAuditLog();

        mockMvc.perform(get(AUDIT_URL + "/" + auditLog.getId())
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isOk());

        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_AUDIT_URL + EMAIL_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Audit Log delete response does not match")
            .isEqualTo("2 audit log(s) deleted with user email starting with " + EMAIL_PREFIX);

        mockMvc.perform(get(AUDIT_URL + "/" + auditLog.getId())
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void testTestingSupportUpdateAuditLogTimestampWithId() throws Exception {
        AuditLog auditLog = createAuditLog();
        LocalDate expiredDate = auditLog.getTimestamp().minusDays(200).toLocalDate();

        mockMvc.perform(get(AUDIT_URL + "/" + auditLog.getId())
                            .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isOk());

        MvcResult updateAudit = mockMvc.perform(put(TESTING_SUPPORT_AUDIT_URL + auditLog.getId())
                                                    .header(REQUESTER_ID_HEADER, REQUESTER_ID))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(updateAudit.getResponse().getContentAsString())
            .as("Audit Log update response does not match")
            .contains("1 audit log(s) updated with timestamp " + expiredDate);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedTestingSupportDeleteAccounts() throws Exception {
        mockMvc.perform(delete(TESTING_SUPPORT_ACCOUNT_URL + EMAIL_PREFIX))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedTestingSupportDeleteApplications() throws Exception {
        mockMvc.perform(delete(TESTING_SUPPORT_APPLICATION_URL + EMAIL_PREFIX))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedTestingSupportDeleteSubscriptions() throws Exception {
        mockMvc.perform(delete(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME_PREFIX))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedTestingSupportDeleteThirdPartyUsers() throws Exception {
        mockMvc.perform(delete(TESTING_SUPPORT_THIRD_PARTY_URL + NAME_PREFIX))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedTestingSupportDeleteAudits() throws Exception {
        mockMvc.perform(delete(TESTING_SUPPORT_AUDIT_URL + EMAIL_PREFIX))
            .andExpect(status().isForbidden());
    }

    private PiUser createUser() {
        PiUser user = new PiUser();
        user.setEmail(EMAIL);
        user.setProvenanceUserId(PROVENANCE_USER_ID);
        user.setUserProvenance(PROVENANCE);
        user.setRoles(ROLE);
        user.setForenames(GIVEN_NAME);
        user.setSurname(SURNAME);
        return user;
    }

    private AzureAccount createAccount(String password) {
        AzureAccount newAccount = new AzureAccount();
        newAccount.setEmail(EMAIL);
        newAccount.setPassword(password);
        newAccount.setDisplayName(GIVEN_NAME);
        newAccount.setFirstName(GIVEN_NAME);
        newAccount.setSurname(SURNAME);
        return newAccount;
    }

    private MediaApplication createApplication() throws Exception {
        MediaApplication application = new MediaApplication();
        application.setFullName(FULL_NAME);
        application.setEmail(EMAIL);
        application.setEmployer(EMPLOYER);
        application.setStatus(PENDING_STATUS);

        try (InputStream imageInputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("files/test-image.png")) {

            MockMultipartFile imageFile = new MockMultipartFile("file", "test-image.png",
                                                                "", imageInputStream
            );

            MockHttpServletRequestBuilder postRequest = multipart(APPLICATION_URL)
                .file(imageFile)
                .flashAttr("application", application)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE);

            MvcResult mvcResult = mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andReturn();

            return OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), MediaApplication.class);
        }
    }

    private Subscription createSubscription() {
        Subscription subscription = new Subscription();

        subscription.setLocationName(LOCATION_NAME_PREFIX + LOCATION_NAME);
        subscription.setChannel(Channel.API_COURTEL);
        subscription.setSearchType(SearchType.CASE_ID);
        subscription.setSearchValue(CASE_ID);
        subscription.setCaseNumber(CASE_ID);
        subscription.setCreatedDate(LocalDateTime.now());
        subscription.setUserId(USER_ID);

        return subscription;
    }

    private ApiUser creatThirdPartyUser() {
        ApiUser apiUser = new ApiUser();
        apiUser.setName(NAME_PREFIX + FULL_NAME);
        return apiUser;
    }

    private ApiSubscription creatThirdPartySubscription(UUID userId) {
        ApiSubscription apiSubscription = new ApiSubscription();
        apiSubscription.setUserId(userId);
        apiSubscription.setListType(ListType.CIVIL_AND_FAMILY_DAILY_CAUSE_LIST);
        apiSubscription.setSensitivity(Sensitivity.PUBLIC);
        return apiSubscription;
    }

    private ApiOauthConfiguration creatThirdPartyOauthConfiguration(UUID userId) {
        ApiOauthConfiguration apiOauthConfiguration = new ApiOauthConfiguration();
        apiOauthConfiguration.setUserId(userId);
        apiOauthConfiguration.setDestinationUrl(DESTINATION_URL);
        apiOauthConfiguration.setTokenUrl(TOKEN_URL);
        apiOauthConfiguration.setClientIdKey(CLIENT_ID_KEY);
        apiOauthConfiguration.setClientSecretKey(CLIENT_SECRET_KEY);
        apiOauthConfiguration.setScopeKey(SCOPE_KEY);
        return apiOauthConfiguration;
    }

    private AuditLog createAuditLog() throws Exception {
        AuditLog auditLog = new AuditLog();
        auditLog.setUserId(ID);
        auditLog.setUserEmail(EMAIL);
        auditLog.setRoles(ROLE);
        auditLog.setUserProvenance(PROVENANCE);
        auditLog.setAction(ACTION);
        auditLog.setDetails(DETAILS);

        MockHttpServletRequestBuilder postRequest = MockMvcRequestBuilders
            .post(AUDIT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(auditLog))
            .header(REQUESTER_ID_HEADER, SYSTEM_ADMIN_USER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(postRequest)
            .andExpect(status().isOk())
            .andReturn();

        return OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), AuditLog.class);
    }

}
