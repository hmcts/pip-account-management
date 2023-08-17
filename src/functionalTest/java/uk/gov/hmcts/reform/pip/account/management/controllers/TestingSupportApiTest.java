package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import com.microsoft.graph.requests.UserRequest;
import com.microsoft.graph.requests.UserRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationDto;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})

@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.JUnitTestsShouldIncludeAssert"})
class TestingSupportApiTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TESTING_SUPPORT_BASE_URL = "/testing-support/";
    private static final String TESTING_SUPPORT_ACCOUNT_URL = TESTING_SUPPORT_BASE_URL + "account/";
    private static final String TESTING_SUPPORT_APPLICATION_URL = TESTING_SUPPORT_BASE_URL + "application/";
    private static final String TESTING_SUPPORT_CREATE_ACCOUNT_URL = TESTING_SUPPORT_BASE_URL + "account";

    private static final String ACCOUNT_URL = "/account/";
    private static final String ACCOUNT_ADD_USER_URL = ACCOUNT_URL + "add/pi";
    private static final String APPLICATION_URL = "/application";
    private static final String BLOB_IMAGE_URL = "https://localhost";
    private static final String B2C_URL = "URL";

    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String ISSUER_ID = "1234-1234-1234-1234";

    private static final String EMAIL_PREFIX = "TEST_789_";
    private static final String EMAIL = EMAIL_PREFIX + "user123@test.com";
    private static final String PASSWORD = "P@55word11";
    private static final String ID = "1234";

    private static final String PROVENANCE_USER_ID = UUID.randomUUID().toString();
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final Roles ROLE = Roles.VERIFIED;
    private static final String GIVEN_NAME = "Given Name";
    private static final String SURNAME = "Surname";

    private static final String FULL_NAME = "Test user";
    private static final String EMPLOYER = "Test employer";
    private static final String PENDING_STATUS = "PENDING";

    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    BlobContainerClient blobContainerClient;

    @Autowired
    BlobClient blobClient;

    @Autowired
    GraphServiceClient<Request> graphClient;

    @Mock
    private UserRequestBuilder userRequestBuilder;

    @Mock
    private UserRequest userRequest;

    @Autowired
    UserCollectionRequestBuilder userCollectionRequestBuilder;

    @Autowired
    UserCollectionRequest userCollectionRequest;

    @Autowired
    GraphServiceException graphServiceException;

    @Mock
    private UserCollectionPage userCollectionPage;

    @Mock
    private ClientConfiguration clientConfiguration;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @Test
    void testTestingSupportCreateAccount() throws Exception {

        //Azure mock setup
        User userToReturn = new User();
        userToReturn.id = ID;
        userToReturn.givenName = GIVEN_NAME;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        //Create new test user account
        AzureAccount newAccount = createAccount(PASSWORD);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(TESTING_SUPPORT_CREATE_ACCOUNT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(newAccount))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult postResponse = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isCreated())
            .andReturn();

        PiUser createdAccount = OBJECT_MAPPER.readValue(postResponse.getResponse().getContentAsString(), PiUser.class);

        assertEquals(EMAIL, createdAccount.getEmail(),
                     "Azure account creation error"
        );

        //Reset azure mock setup
        Mockito.reset(graphClient, userCollectionRequest, userCollectionRequestBuilder);

        //User mock setup
        when(graphClient.users(any())).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.get()).thenReturn(new User());

        //Check whether account is created in Pi user table
        MockHttpServletRequestBuilder getPiUserRequest = get(ACCOUNT_URL + createdAccount.getUserId());

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
        mockMvc.perform(get(ACCOUNT_URL + createdAccount.getUserId()))
            .andExpect(status().isNotFound());
    }

    @Test
    void testBadRequestTestingSupportCreateAccount() throws Exception {

        User userToReturn = new User();
        userToReturn.id = ID;
        userToReturn.givenName = GIVEN_NAME;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        AzureAccount newAccount = createAccount("");

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(TESTING_SUPPORT_CREATE_ACCOUNT_URL)
            .content(OBJECT_MAPPER.writeValueAsString(newAccount))
            .header(ISSUER_HEADER, ISSUER_ID)
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
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isForbidden());
    }


    @Test
    void testTestingSupportDeleteAccountsWithEmailPrefix() throws Exception {
        when(graphClient.users(any())).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.get()).thenReturn(new User());

        PiUser user = createUser();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(ACCOUNT_ADD_USER_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(user)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult postResponse = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isCreated())
            .andReturn();

        ConcurrentHashMap<CreationEnum, List<String>> mappedResponse = OBJECT_MAPPER.readValue(
            postResponse.getResponse().getContentAsString(), new TypeReference<>() {
            }
        );

        assertThat(mappedResponse.get(CreationEnum.CREATED_ACCOUNTS))
            .as("No account created")
            .hasSize(1);

        String userId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0);
        mockMvc.perform(get(ACCOUNT_URL + userId))
            .andExpect(status().isOk());

        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_ACCOUNT_URL + EMAIL_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Media application delete response does not match")
            .isEqualTo("1 account(s) deleted with email starting with " + EMAIL_PREFIX);

        mockMvc.perform(get(ACCOUNT_URL + userId))
            .andExpect(status().isNotFound());
    }

    @Test
    void testTestingSupportDeleteApplicationsWithEmailPrefix() throws Exception {
        MediaApplication application = createApplication();

        mockMvc.perform(get(APPLICATION_URL + "/" + application.getId()))
            .andExpect(status().isOk());

        MvcResult deleteResponse = mockMvc.perform(delete(TESTING_SUPPORT_APPLICATION_URL + EMAIL_PREFIX))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(deleteResponse.getResponse().getContentAsString())
            .as("Media application delete response does not match")
            .isEqualTo("1 media application(s) deleted with email starting with " + EMAIL_PREFIX);

        mockMvc.perform(get(APPLICATION_URL + "/" + application.getId()))
            .andExpect(status().isNotFound());
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
        newAccount.setRole(ROLE);
        newAccount.setFirstName(GIVEN_NAME);
        newAccount.setSurname(SURNAME);
        return newAccount;
    }

    private MediaApplication createApplication() throws Exception {
        MediaApplicationDto applicationDto = new MediaApplicationDto();
        applicationDto.setFullName(FULL_NAME);
        applicationDto.setEmail(EMAIL);
        applicationDto.setEmployer(EMPLOYER);
        applicationDto.setStatus(PENDING_STATUS);

        try (InputStream imageInputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("files/test-image.png")) {

            MockMultipartFile imageFile = new MockMultipartFile("file", "test-image.png",
                                                                "", imageInputStream
            );

            when(blobContainerClient.getBlobClient(any())).thenReturn(blobClient);
            when(blobContainerClient.getBlobContainerUrl()).thenReturn(BLOB_IMAGE_URL);

            MockHttpServletRequestBuilder postRequest = multipart(APPLICATION_URL)
                .file(imageFile)
                .flashAttr("application", applicationDto)
                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE);

            MvcResult mvcResult = mockMvc.perform(postRequest)
                .andExpect(status().isOk())
                .andReturn();

            return OBJECT_MAPPER.readValue(mvcResult.getResponse().getContentAsString(), MediaApplication.class);
        }
    }
}
