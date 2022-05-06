package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTest;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTest.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "test")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = { "APPROLE_api.request.admin" })
@SuppressWarnings({"PMD.TooManyMethods", "PMD.LawOfDemeter", "PMD.ExcessiveImports"})
class AccountTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    GraphServiceClient<Request> graphClient;

    @Autowired
    UserCollectionRequestBuilder userCollectionRequestBuilder;

    @Autowired
    UserCollectionRequest userCollectionRequest;

    @Autowired
    GraphServiceException graphServiceException;

    private static final String ROOT_URL = "/account";
    private static final String AZURE_URL = ROOT_URL + "/add/azure";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String GET_PROVENANCE_USER_URL = ROOT_URL + "/provenance/";
    private static final String EMAIL = "a@b";
    private static final String INVALID_EMAIL = "ab";
    private static final String FIRST_NAME = "First name";
    private static final String SURNAME = "Surname";
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final Roles ROLE = Roles.INTERNAL_ADMIN_CTSC;
    private static final String ISSUER_EMAIL = "issuer@email.com";
    private static final String ISSUER_HEADER = "x-issuer-email";

    private static final String ID = "1234";

    private static final String EMAIL_VALIDATION_MESSAGE = "email: Invalid email provided. "
        + "Email must contain an @ symbol";
    private static final String INVALID_FIRST_NAME_MESSAGE = "firstName: must not be empty";
    private static final String INVALID_SURNAME_MESSAGE = "surname: must not be empty";
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
    private static final String ERROR_RESPONSE_FORBIDDEN =
        "User: %s does not have sufficient permission to view list type: %s";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";

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
        validUser = createUser(true, UUID.randomUUID().toString());
    }

    @AfterEach
    public void reset() {
        Mockito.reset(graphClient, userCollectionRequest, userCollectionRequestBuilder);
    }

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    void creationOfValidAccount() throws Exception {

        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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

    @Test
    void testCreationOfNoEmailAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
    void testCreationOfNoSurnameAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
        ErroredAzureAccount erroredAccount = objectMapper.convertValue(
            accountList.get(0),
            ErroredAzureAccount.class);

        assertNull(erroredAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertNull(erroredAccount.getSurname(), "Surname has been sent");
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(INVALID_SURNAME_MESSAGE, erroredAccount.getErrorMessages().get(0),
                     "Error message is displayed for an invalid name");
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(), "1 User should be created");
    }

    @Test
    void testCreateMultipleSuccessUsers() throws Exception {
        PiUser validUser1 = createUser(true, UUID.randomUUID().toString());
        PiUser validUser2 = createUser(true, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser1, validUser2)))
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
    void testIsUserAuthenticatedReturnsSuccessful() throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_EMAIL)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult userResponse = mockMvc.perform(setupRequest).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(userResponse.getResponse().getContentAsString(),
                                   new TypeReference<>() {});
        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();


        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format("%s/isAuthorised/%s/%s", ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST));

        MvcResult response = mockMvc.perform(request).andExpect(status().isOk()).andReturn();

        assertEquals("true", response.getResponse().getContentAsString(), "Should return true");
    }

    @Test
    void testIsUserAuthenticatedReturnsForbidden() throws Exception {
        validUser.setUserProvenance(UserProvenances.CFT_IDAM);
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(objectMapper.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_EMAIL)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult userResponse = mockMvc.perform(setupRequest).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            objectMapper.readValue(userResponse.getResponse().getContentAsString(),
                                   new TypeReference<>() {});
        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format("%s/isAuthorised/%s/%s", ROOT_URL, createdUserId, ListType.SJP_PRESS_LIST));

        MvcResult response = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertTrue(response.getResponse().getContentAsString()
                       .contains(String.format(ERROR_RESPONSE_FORBIDDEN, createdUserId, ListType.SJP_PRESS_LIST)),
                   "Should return forbidden message");
    }

    @Test
    @WithMockUser(username = "unauthorized_account", authorities = { "APPROLE_unknown.account" })
    void testUnauthorizedCreateAccount() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content("[]")
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .header(ISSUER_HEADER, ISSUER_EMAIL)
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
            .get(String.format("%s/isAuthorised/%s/%s", ROOT_URL, UUID.randomUUID(), ListType.SJP_PRESS_LIST));

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE);
    }

}
