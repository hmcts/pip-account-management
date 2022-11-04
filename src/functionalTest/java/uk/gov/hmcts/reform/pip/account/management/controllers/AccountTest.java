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
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTest;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTest.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@WithMockUser(username = "admin", authorities = { "APPROLE_api.request.admin" })
@SuppressWarnings({"PMD.TooManyMethods", "PMD.LawOfDemeter", "PMD.ExcessiveImports",
    "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveClassLength"})
class AccountTest {

    @Autowired
    BlobContainerClient blobContainerClient;

    @Autowired
    BlobClient blobClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    GraphServiceClient<Request> graphClient;

    @Autowired
    UserCollectionRequestBuilder userCollectionRequestBuilder;

    @Autowired
    UserCollectionRequest userCollectionRequest;

    @Mock
    private UserRequestBuilder userRequestBuilder;

    @Mock
    private UserRequest userRequest;

    @Mock
    private UserCollectionPage userCollectionPage;

    @Autowired
    GraphServiceException graphServiceException;

    @Mock
    private ClientConfiguration clientConfiguration;

    private static final String ROOT_URL = "/account";
    private static final String AZURE_URL = ROOT_URL + "/add/azure";
    private static final String BULK_UPLOAD = ROOT_URL + "/media-bulk-upload";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String CREATE_MEDIA_USER_URL = "/application";
    private static final String GET_PROVENANCE_USER_URL = ROOT_URL + "/provenance/";
    private static final String UPDATE_ACCOUNT_URL = ROOT_URL + "/provenance/";
    private static final String NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive/notify";
    private static final String DELETE_EXPIRED_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive";
    private static final String NOTIFY_INACTIVE_ADMIN_ACCOUNTS_URL = ROOT_URL + "/admin/inactive/notify";
    private static final String DELETE_EXPIRED_ADMIN_ACCOUNTS_URL = ROOT_URL + "/admin/inactive";
    private static final String NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive/notify";
    private static final String DELETE_EXPIRED_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive";
    private static final String MI_REPORTING_ACCOUNT_DATA_URL = ROOT_URL + "/mi-data";
    private static final String EMAIL_URL = ROOT_URL + "/emails";
    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String INVALID_EMAIL = "ab";
    private static final String FIRST_NAME = "First name";
    private static final String SURNAME = "Surname";
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final Roles ROLE = Roles.INTERNAL_ADMIN_CTSC;
    private static final String ISSUER_ID = "1234-1234-1234-1234";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String MEDIA_LIST = "mediaList";
    private static final String GIVEN_NAME = "Given Name";
    private static final String B2C_URL = "URL";

    private static final String ID = "1234";
    private static final String ADDITIONAL_ID = "4321";

    private static final String EMAIL_VALIDATION_MESSAGE = "email: must be a well-formed email address";
    private static final String INVALID_FIRST_NAME_MESSAGE = "firstName: must not be empty";
    private static final String INVALID_ROLE_MESSAGE = "role: must not be null";
    private static final String DIRECTORY_ERROR = "Error when persisting account into Azure. "
        + "Check that the user doesn't already exist in the directory";
    private static final String TEST_MESSAGE_ID = "AzureAccount ID added to account";
    private static final String TEST_MESSAGE_EMAIL = "Email matches sent account";
    private static final String TEST_MESSAGE_FIRST_NAME = "Firstname matches sent account";
    private static final String TEST_MESSAGE_SURNAME = "Surname matches sent account";
    private static final String TEST_MESSAGE_ROLE = "Role matches sent account";
    private static final String ZERO_CREATED_ACCOUNTS = "0 created accounts should be returned";
    private static final String SINGLE_ERRORED_ACCOUNT = "1 errored account should be returned";
    private static final String ERROR_RESPONSE_USER_PROVENANCE = "No user found with the provenanceUserId: 1234";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String TEST_UUID_STRING = UUID.randomUUID().toString();
    private static final String MAP_SIZE_MESSAGE = "Map size should match";

    private ObjectMapper objectMapper;

    private PiUser validUser;

    private PiUser createUser(boolean valid, String id) {
        PiUser user = new PiUser();
        user.setEmail(valid ? EMAIL : INVALID_EMAIL);
        user.setProvenanceUserId(id);
        user.setUserProvenance(PROVENANCE);
        user.setRoles(ROLE);

        return user;
    }

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        validUser = createUser(true, UUID.randomUUID().toString());

        User userToReturn = new User();
        userToReturn.id = ID;
        userToReturn.givenName = GIVEN_NAME;
        User additionalUser = new User();
        additionalUser.id = ADDITIONAL_ID;
        additionalUser.givenName = GIVEN_NAME;
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn, additionalUser);
    }

    @AfterEach
    public void reset() {
        Mockito.reset(graphClient, userCollectionRequest, userCollectionRequestBuilder);
    }

    @Test
    void creationOfValidAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();


        ConcurrentHashMap<CreationEnum, List<AzureAccount>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(0, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "No errored account should be returned");
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Created account should be returned");

        AzureAccount returnedAzureAccount = accounts.get(CreationEnum.CREATED_ACCOUNTS).get(0);

        assertEquals(ID, returnedAzureAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, returnedAzureAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedAzureAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedAzureAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, returnedAzureAccount.getRole(), TEST_MESSAGE_ROLE);
    }

    @Test
    void creationOfDuplicateAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.VERIFIED);

        User user = new User();
        user.id = ID;
        user.givenName = GIVEN_NAME;
        List<User> azUsers = new ArrayList<>();
        azUsers.add(user);

        userCollectionPage = new UserCollectionPage(azUsers, userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(0, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT);
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS);
    }

    @Test
    void testCreationOfInvalidEmailAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail("ab");
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT);
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS);

        List<Object> accountList = accounts.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredAzureAccount erroredAccount = objectMapper.convertValue(accountList.get(0),
                                                                          ErroredAzureAccount.class);

        assertNull(erroredAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals("ab", erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, erroredAccount.getErrorMessages().get(0),
                   "Error message is displayed for an invalid email");

    }

    @Ignore
    void testCreationOfNoEmailAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT);
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS);

        List<Object> accountList = accounts.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredAzureAccount erroredAccount = objectMapper.convertValue(accountList.get(0),
                                                                          ErroredAzureAccount.class);

        assertNull(erroredAccount.getAzureAccountId(),  TEST_MESSAGE_ID);
        assertNull(erroredAccount.getEmail(), "Email has not been sent");
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, erroredAccount.getErrorMessages().get(0),
                     "Error message is displayed for an invalid email");
    }

    @Test
    void testCreationOfNoFirstnameAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT);
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS);

        List<Object> accountList = accounts.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredAzureAccount erroredAccount = objectMapper.convertValue(accountList.get(0),
                                                                          ErroredAzureAccount.class);

        assertNull(erroredAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertNull(erroredAccount.getFirstName(), "Firstname has not been sent");
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(INVALID_FIRST_NAME_MESSAGE, erroredAccount.getErrorMessages().get(0),
                     "Error message is displayed for an invalid name");
    }

    @Test
    void testNoFailureOfNoSurnameAccount() throws Exception {
        User userToReturn = new User();
        userToReturn.id = ID;
        userToReturn.givenName = GIVEN_NAME;

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.VERIFIED);

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(0, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT);
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS);
    }

    @Test
    void testCreationOfNoRoleAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setSurname(SURNAME);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT);
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS);

        List<Object> acccountList = accounts.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredAzureAccount erroredAccount = objectMapper.convertValue(acccountList.get(0),
                                                                          ErroredAzureAccount.class);

        assertNull(erroredAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertNull(erroredAccount.getRole(), "Role is not null");
        assertEquals(INVALID_ROLE_MESSAGE, erroredAccount.getErrorMessages().get(0),
                     "Error message is displayed for an invalid name");
    }

    @Test
    void testCreationOfDuplicateAccount() throws Exception {

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenThrow(graphServiceException);

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(), "1 errored account returned");
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(), "0 created account returned");

        ErroredAzureAccount erroredAccount = objectMapper.convertValue(
            accounts.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredAzureAccount.class);

        assertNull(erroredAccount.getAzureAccountId(), "Errored azureAccount does not have ID");
        assertEquals(EMAIL, erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(DIRECTORY_ERROR, erroredAccount.getErrorMessages().get(0),
                     "Error message matches directory message");
    }

    @Test
    void testBadRequestWhenCreatingAUserWithADuplicateEmailKey() throws Exception {
        String duplicateKeyString = "[{\"email\": \"a@b.com\", \"email\": \"a@b.com\"}]";

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(duplicateKeyString)
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest()).andReturn();

        objectMapper.findAndRegisterModules();

        ExceptionResponse exceptionResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(), ExceptionResponse.class);

        assertTrue(exceptionResponse.getMessage().contains("Duplicate field 'email'"), "Duplicate email "
            + "error message must be displayed when duplicate keys exist");
    }

    @Test
    void testCreationOfTwoAccountsOneFailOneOK() throws Exception {

        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        AzureAccount validAzureAccount = new AzureAccount();
        validAzureAccount.setEmail(EMAIL);
        validAzureAccount.setSurname(SURNAME);
        validAzureAccount.setFirstName(FIRST_NAME);
        validAzureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        AzureAccount invalidAzureAccount = new AzureAccount();
        invalidAzureAccount.setEmail("abc.test");
        invalidAzureAccount.setSurname(SURNAME);
        invalidAzureAccount.setFirstName(FIRST_NAME);
        invalidAzureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(validAzureAccount, invalidAzureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> accounts =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 Errored account should be returned");
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Created account should be returned");

        AzureAccount returnedValidAzureAccount = objectMapper.convertValue(
            accounts.get(CreationEnum.CREATED_ACCOUNTS).get(0), AzureAccount.class);

        assertEquals(ID, returnedValidAzureAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, returnedValidAzureAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedValidAzureAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedValidAzureAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, returnedValidAzureAccount.getRole(), TEST_MESSAGE_ROLE);

        ErroredAzureAccount returnedInvalidAccount = objectMapper.convertValue(
            accounts.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredAzureAccount.class);

        assertNull(returnedInvalidAccount.getAzureAccountId(), "AzureAccount ID should be null");
        assertEquals("abc.test", returnedInvalidAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedInvalidAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedInvalidAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, returnedInvalidAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, returnedInvalidAccount.getErrorMessages().get(0),
                     "Error message is displayed for an invalid email");
    }

    @Test
    void testCreateSingleUser() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(), "1 User should be created");
    }

    @Test
    void testCreateMultipleSuccessUsers() throws Exception {

        User userToReturn = new User();
        userToReturn.id = ID;
        userToReturn.givenName = GIVEN_NAME;

        when(graphClient.users(any())).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.get()).thenReturn(userToReturn);

        MockHttpServletRequestBuilder mockHttpServletRequestMediaUserBuilder = MockMvcRequestBuilders
            .get(CREATE_MEDIA_USER_URL)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestMediaUserBuilder).andExpect(status().isOk()).andReturn();

        PiUser validUser1 = createUser(true, UUID.randomUUID().toString());
        PiUser validUser2 = createUser(true, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder =
            MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser1, validUser2)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(2, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(), "1 Users should be created");
    }

    @Test
    void testCreateMultipleSuccessUsersWithDifferentEmails() throws Exception {
        User userToReturn = new User();
        userToReturn.id = ID;
        userToReturn.givenName = GIVEN_NAME;

        when(graphClient.users(any())).thenReturn(userRequestBuilder);
        when(userRequestBuilder.buildRequest()).thenReturn(userRequest);
        when(userRequest.get()).thenReturn(userToReturn);

        MockHttpServletRequestBuilder mockHttpServletRequestMediaUserBuilder = MockMvcRequestBuilders
            .get(CREATE_MEDIA_USER_URL)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestMediaUserBuilder).andExpect(status().isOk()).andReturn();

        PiUser validUser1 = createUser(true, UUID.randomUUID().toString());
        PiUser validUser2 = new PiUser();
        validUser2.setEmail("a@test.com");
        validUser2.setProvenanceUserId(UUID.randomUUID().toString());
        validUser2.setUserProvenance(PROVENANCE);
        validUser2.setRoles(ROLE);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(objectMapper.writeValueAsString(List.of(validUser1, validUser2)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(2, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(), "2 Users should be created");
    }

    @Test
    void testCreateSingleErroredUser() throws Exception {
        PiUser invalidUser = createUser(false, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(invalidUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(), "1 User should be errored");
    }

    @Test
    void testCreateMultipleErroredUsers() throws Exception {
        PiUser invalidUser1 = createUser(false, UUID.randomUUID().toString());
        PiUser invalidUser2 = createUser(false, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(invalidUser1, invalidUser2)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(2, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(), "2 Users should be errored");
    }

    @Test
    void testCreateMultipleUsersCreateAndErrored() throws Exception {
        PiUser invalidUser = createUser(false, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser, invalidUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(), "1 User should be created");
        assertEquals(1, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(), "1 User should be errored");
    }

    @Test
    void testGetUserByProvenanceIdReturnsUser() throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(String.format("%s/%s/%s", GET_PROVENANCE_USER_URL, validUser.getUserProvenance(),
                               validUser.getProvenanceUserId()))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        PiUser returnedUser = objectMapper.readValue(response.getResponse().getContentAsString(),
                                                     PiUser.class);
        assertEquals(validUser.getProvenanceUserId(), returnedUser.getProvenanceUserId(), "Users should match");
        assertThat(returnedUser.getCreatedDate()).as("Created date must not be null").isNotNull();
    }

    @Test
    void testGetUserByProvenanceIdReturnsNotFound() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(String.format("%s/%s/%s", GET_PROVENANCE_USER_URL, UserProvenances.CFT_IDAM, ID))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isNotFound()).andReturn();
        assertEquals(404, response.getResponse().getStatus(), "Status codes should match");
        assertTrue(response.getResponse().getContentAsString().contains(ERROR_RESPONSE_USER_PROVENANCE),
                   "Should contain error message");
    }

    @Test
    @WithMockUser(username = "unauthorized_account", authorities = { "APPROLE_unknown.account" })
    void testUnauthorizedCreateAccount() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content("[]")
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = "unauthroized_user", authorities = { "APPROLE_unknown.user" })
    void testUnauthorizedCreateUser() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content("[]")
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = "unauthorized_provenance", authorities = { "APPROLE_unknown.provenance" })
    void testUnauthorizedGetUserByProvenance() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(String.format("%s/%s/%s", GET_PROVENANCE_USER_URL, UserProvenances.CFT_IDAM, ID))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    @WithMockUser(username = "unauthorized_isAuthorized", authorities = { "APPROLE_unknown.authorized" })
    void testUnauthorizedGetUserIsAuthorized() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format("%s/isAuthorised/%s/%s/%s", ROOT_URL, UUID.randomUUID(),
                               ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC));

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

    @Test
    void testGetUserEmailsByIds() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(EMAIL_URL)
            .content(objectMapper.writeValueAsString(List.of(TEST_UUID_STRING)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        assertEquals(HttpStatus.OK.value(), mvcResult.getResponse().getStatus(),
                     "Status codes does match OK");
    }

    @Test
    void testUploadBulkMedia() throws Exception {

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/valid.csv")) {

            MockMultipartFile multipartFile = new MockMultipartFile(MEDIA_LIST,
                                                                IOUtils.toByteArray(inputStream));

            MvcResult mvcResult = mockMvc.perform(multipart(BULK_UPLOAD).file(multipartFile)
                                                      .header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isOk()).andReturn();
            ConcurrentHashMap<CreationEnum, List<?>> users = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {});

            assertEquals(2, users.get(CreationEnum.CREATED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
            assertEquals(0, users.get(CreationEnum.ERRORED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);

        }
    }

    @Test
    void testUploadBulkMediaFailsCsv() throws Exception {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/invalidCsv.txt")) {
            MockMultipartFile csvFile
                = new MockMultipartFile(MEDIA_LIST, inputStream);

            MvcResult result = mockMvc.perform(multipart(BULK_UPLOAD).file(csvFile).header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isBadRequest()).andReturn();

            assertTrue(result.getResponse().getContentAsString().contains("Failed to parse CSV File"),
                       "Should contain error");
        }
    }

    @Test
    void testUploadBulkMediaEmailOnly() throws Exception {

        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);
        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/mediaEmailOnly.csv")) {

            MockMultipartFile multipartFile = new MockMultipartFile(MEDIA_LIST,
                                                                    IOUtils.toByteArray(inputStream));

            MvcResult mvcResult = mockMvc.perform(multipart(BULK_UPLOAD).file(multipartFile)
                                                      .header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isOk()).andReturn();
            ConcurrentHashMap<CreationEnum, List<?>> users = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {});

            assertEquals(2, users.get(CreationEnum.CREATED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
            assertEquals(0, users.get(CreationEnum.ERRORED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);

        }
    }

    @Test
    void testUploadBulkMediaEmailValidation() throws Exception {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/invalidEmail.csv")) {

            MockMultipartFile multipartFile = new MockMultipartFile(MEDIA_LIST,
                                                                    IOUtils.toByteArray(inputStream));

            MvcResult mvcResult = mockMvc.perform(multipart(BULK_UPLOAD).file(multipartFile)
                                                      .header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isOk()).andReturn();
            ConcurrentHashMap<CreationEnum, List<?>> users = objectMapper.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {});

            assertEquals(0, users.get(CreationEnum.CREATED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
            assertEquals(1, users.get(CreationEnum.ERRORED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);

        }
    }

    @Test
    void testUpdateAccountLastVerifiedDateSuccessful() throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                     + validUser.getProvenanceUserId())
            .content(objectMapper.writeValueAsString(Collections.singletonMap(
                "lastVerifiedDate", "2022-08-14T20:21:10.912Z")))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
                "has been updated")));
    }

    @Test
    void testUpdateAccountLastSignedInDateSuccessful() throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                     + validUser.getProvenanceUserId())
            .content(objectMapper.writeValueAsString(Collections.singletonMap(
                "lastSignedInDate", "2022-08-14T20:21:10.912Z")))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk())
            .andExpect(content().string(containsString(
            "has been updated")));
    }

    @Test
    void testUpdateAccountNotFound() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/1234")
            .content(objectMapper.writeValueAsString(Map.of(
                "lastSignedInDate", "2022-08-14T20:21:20.912Z")))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString(
                "User with supplied provenance id: 1234 could not be found")));
    }

    @Test
    void testUpdateAccountWithUnsupportedParam() throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                + validUser.getProvenanceUserId())
            .content(objectMapper.writeValueAsString(Collections.singletonMap(
                "email", "test@test.com")))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("The field 'email' could not be updated")));
    }

    @Test
    void testNotifyInactiveMediaAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    void testDeleteExpiredMediaAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_MEDIA_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    void testNotifyInactiveAdminAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_ADMIN_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    void testDeleteExpiredAdminAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_ADMIN_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    void testNotifyInactiveIdamAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    void testDeleteExpiredIdamAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_IDAM_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    void testMiAccountDataRequestSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(MI_REPORTING_ACCOUNT_DATA_URL);

        mockMvc.perform(request).andExpect(status().isOk());
    }
}
