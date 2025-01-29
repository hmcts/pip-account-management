package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserCollectionResponse;
import com.microsoft.graph.serviceclient.GraphServiceClient;
import com.microsoft.graph.users.UsersRequestBuilder;
import com.microsoft.kiota.ApiException;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.ClientConfiguration;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredAzureAccount;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {Application.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports"})
class AzureAccountTest extends IntegrationTestBase {
    private static final String ROOT_URL = "/account";
    private static final String AZURE_URL = ROOT_URL + "/add/azure";
    private static final String B2C_URL = "URL";
    private static final String AZURE_PATH = "/azure/";

    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String INVALID_EMAIL = "ab";
    private static final String FIRST_NAME = "First name";
    private static final String SURNAME = "Surname";
    private static final String ISSUER_ID = "1234-1234-1234-1234";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String GIVEN_NAME = "Given Name";
    private static final String ID = "1234";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String EMAIL_VALIDATION_MESSAGE = "email: must be a well-formed email address";
    private static final String INVALID_FIRST_NAME_MESSAGE = "firstName: must not be empty";
    private static final String INVALID_ROLE_MESSAGE = "role: must not be null";
    private static final String UNSENT_EMAIL_MESSAGE = "Account has been successfully created, however "
        + "email has failed to send.";
    private static final String DIRECTORY_ERROR = "Error when persisting account into Azure. "
        + "Check that the user doesn't already exist in the directory";
    private static final String TEST_MESSAGE_ID = "AzureAccount ID added to account";
    private static final String TEST_MESSAGE_EMAIL = "Email matches sent account";
    private static final String TEST_MESSAGE_FIRST_NAME = "Firstname matches sent account";
    private static final String TEST_MESSAGE_SURNAME = "Surname matches sent account";
    private static final String TEST_MESSAGE_ROLE = "Role matches sent account";
    private static final String ZERO_CREATED_ACCOUNTS = "0 created accounts should be returned";
    private static final String SINGLE_ERRORED_ACCOUNT = "1 errored account should be returned";
    private static final String NOT_FOUND_STATUS_CODE_MESSAGE = "Status code does not match not found";
    private static final String INVALID_EMAIL_ERROR = "Error message is displayed for an invalid email";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String MESSAGE_ERROR = "Error message does not match";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final PiUser VALID_USER = createUser(true, UUID.randomUUID().toString());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    GraphServiceClient graphClient;

    @Autowired
    UsersRequestBuilder usersRequestBuilder;

    @Autowired
    ApiException apiException;

    @Mock
    private ClientConfiguration clientConfiguration;

    private static PiUser createUser(boolean valid, String id) {
        PiUser user = new PiUser();
        user.setEmail(valid ? EMAIL : INVALID_EMAIL);
        user.setProvenanceUserId(id);
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        user.setForenames(FIRST_NAME);
        user.setSurname(SURNAME);

        return user;
    }

    private void mockPiUser() {
        User user = new User();
        user.setId(ID);
        user.setGivenName(GIVEN_NAME);

        List<User> azUsers = new ArrayList<>();
        azUsers.add(user);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(azUsers);

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);
    }

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void setup() {
        User userToReturn = new User();
        userToReturn.setId(ID);
        userToReturn.setGivenName(GIVEN_NAME);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);
    }

    @AfterEach
    public void reset() {
        Mockito.reset(graphClient, usersRequestBuilder);
    }

    @Test
    void creationOfValidAccount() throws Exception {
        when(publicationService.sendNotificationEmail(anyString(), anyString(), anyString())).thenReturn(true);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<AzureAccount>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(0, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "No errored account should be returned"
        );
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Created account should be returned"
        );

        AzureAccount returnedAzureAccount = accounts.get(CreationEnum.CREATED_ACCOUNTS).get(0);

        assertEquals(ID, returnedAzureAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, returnedAzureAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedAzureAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedAzureAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, returnedAzureAccount.getRole(), TEST_MESSAGE_ROLE);
    }

    @Test
    void creationOfValidAccountWhenFailedToSendNotificationEmail() throws Exception {
        when(publicationService.sendNotificationEmail(anyString(), anyString(), anyString())).thenReturn(false);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts = OBJECT_MAPPER.readValue(
            response.getResponse().getContentAsString(),
            new TypeReference<>() {
            });

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "No errored account should be returned"
        );
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Created account should be returned"
        );

        AzureAccount returnedValidAzureAccount = OBJECT_MAPPER.convertValue(
            accounts.get(CreationEnum.CREATED_ACCOUNTS).get(0), AzureAccount.class
        );

        assertEquals(ID, returnedValidAzureAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, returnedValidAzureAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedValidAzureAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedValidAzureAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, returnedValidAzureAccount.getRole(), TEST_MESSAGE_ROLE);

        ErroredAzureAccount returnedInvalidAccount = OBJECT_MAPPER.convertValue(
            accounts.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredAzureAccount.class);

        assertEquals(UNSENT_EMAIL_MESSAGE, returnedInvalidAccount.getErrorMessages().get(0), MESSAGE_ERROR);
    }

    @Test
    void creationOfDuplicateAccount() throws Exception {
        when(publicationService.sendNotificationEmailForDuplicateMediaAccount(anyString(), anyString()))
            .thenReturn(true);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.VERIFIED);

        mockPiUser();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(0, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT
        );
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS
        );
    }

    @Test
    void testCreationOfInvalidEmailAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail("ab");
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT
        );
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS
        );

        List<Object> accountList = accounts.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredAzureAccount erroredAccount = OBJECT_MAPPER.convertValue(
            accountList.get(0),
            ErroredAzureAccount.class
        );

        assertNull(erroredAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals("ab", erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, erroredAccount.getErrorMessages().get(0),
                     INVALID_EMAIL_ERROR
        );
    }

    @Test
    void testCreationOfNoFirstnameAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT
        );
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS
        );

        List<Object> accountList = accounts.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredAzureAccount erroredAccount = OBJECT_MAPPER.convertValue(
            accountList.get(0),
            ErroredAzureAccount.class
        );

        assertNull(erroredAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertNull(erroredAccount.getFirstName(), "Firstname has not been sent");
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(INVALID_FIRST_NAME_MESSAGE, erroredAccount.getErrorMessages().get(0),
                     "Error message is displayed for an invalid name"
        );
    }

    @Test
    void testNoFailureOfNoSurnameAccount() throws Exception {
        when(publicationService.sendMediaNotificationEmail(anyString(), anyString(), anyBoolean())).thenReturn(true);

        User userToReturn = new User();
        userToReturn.setId(ID);
        userToReturn.setGivenName(GIVEN_NAME);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.VERIFIED);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(0, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT
        );
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS
        );
    }

    @Test
    void testNoSurnameAccountWhenFailedToSendNotificationEMail() throws Exception {
        when(publicationService.sendMediaNotificationEmail(anyString(), anyString(), anyBoolean())).thenReturn(false);

        User userToReturn = new User();
        userToReturn.setId(ID);
        userToReturn.setGivenName(GIVEN_NAME);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.VERIFIED);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT
        );
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS
        );

        ErroredAzureAccount returnedInvalidAccount = OBJECT_MAPPER.convertValue(
            accounts.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredAzureAccount.class
        );

        assertEquals(UNSENT_EMAIL_MESSAGE, returnedInvalidAccount.getErrorMessages().get(0), MESSAGE_ERROR);
    }

    @Test
    void testCreationOfNoRoleAccount() throws Exception {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setSurname(SURNAME);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     SINGLE_ERRORED_ACCOUNT
        );
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     ZERO_CREATED_ACCOUNTS
        );

        List<Object> acccountList = accounts.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredAzureAccount erroredAccount = OBJECT_MAPPER.convertValue(
            acccountList.get(0),
            ErroredAzureAccount.class
        );

        assertNull(erroredAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertNull(erroredAccount.getRole(), "Role is not null");
        assertEquals(INVALID_ROLE_MESSAGE, erroredAccount.getErrorMessages().get(0),
                     "Error message is displayed for an invalid name"
        );
    }

    @Test
    void testCreationOfDuplicateAccount() throws Exception {
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenThrow(apiException);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(EMAIL);
        azureAccount.setSurname(SURNAME);
        azureAccount.setFirstName(FIRST_NAME);
        azureAccount.setRole(Roles.INTERNAL_ADMIN_CTSC);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(), "1 errored account returned");
        assertEquals(0, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(), "0 created account returned");

        ErroredAzureAccount erroredAccount = OBJECT_MAPPER.convertValue(
            accounts.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredAzureAccount.class);

        assertNull(erroredAccount.getAzureAccountId(), "Errored azureAccount does not have ID");
        assertEquals(EMAIL, erroredAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, erroredAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(DIRECTORY_ERROR, erroredAccount.getErrorMessages().get(0),
                     "Error message matches directory message"
        );
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

        ExceptionResponse exceptionResponse =
            OBJECT_MAPPER.readValue(response.getResponse().getContentAsString(), ExceptionResponse.class);

        assertTrue(exceptionResponse.getMessage().contains("Duplicate field 'email'"), "Duplicate email "
            + "error message must be displayed when duplicate keys exist");
    }

    @Test
    void testCreationOfTwoAccountsOneFailOneOK() throws Exception {
        when(publicationService.sendNotificationEmail(anyString(), anyString(), anyString())).thenReturn(true);

        User userToReturn = new User();
        userToReturn.setId(ID);

        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.post(any())).thenReturn(userToReturn);

        UserCollectionResponse userCollectionResponse = new UserCollectionResponse();
        userCollectionResponse.setValue(new ArrayList<>());

        when(clientConfiguration.getB2cUrl()).thenReturn(B2C_URL);
        when(graphClient.users()).thenReturn(usersRequestBuilder);
        when(usersRequestBuilder.get(any())).thenReturn(userCollectionResponse);

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
            .content(OBJECT_MAPPER.writeValueAsString(List.of(validAzureAccount, invalidAzureAccount)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        Map<CreationEnum, List<Object>> accounts =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, accounts.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 Errored account should be returned"
        );
        assertEquals(1, accounts.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Created account should be returned"
        );

        AzureAccount returnedValidAzureAccount = OBJECT_MAPPER.convertValue(
            accounts.get(CreationEnum.CREATED_ACCOUNTS).get(0), AzureAccount.class);

        assertEquals(ID, returnedValidAzureAccount.getAzureAccountId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, returnedValidAzureAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedValidAzureAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedValidAzureAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, returnedValidAzureAccount.getRole(), TEST_MESSAGE_ROLE);

        ErroredAzureAccount returnedInvalidAccount = OBJECT_MAPPER.convertValue(
            accounts.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredAzureAccount.class);

        assertNull(returnedInvalidAccount.getAzureAccountId(), "AzureAccount ID should be null");
        assertEquals("abc.test", returnedInvalidAccount.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedInvalidAccount.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedInvalidAccount.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(Roles.INTERNAL_ADMIN_CTSC, returnedInvalidAccount.getRole(), TEST_MESSAGE_ROLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, returnedInvalidAccount.getErrorMessages().get(0),
                     INVALID_EMAIL_ERROR
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedCreateAccount() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content("[]")
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testGetAzureUserInfoNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ROOT_URL + AZURE_PATH + VALID_USER.getProvenanceUserId());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isNotFound()).andReturn();

        assertEquals(NOT_FOUND.value(), mvcResult.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE_MESSAGE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetAzureUserInfo() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ROOT_URL + "/azure/" + VALID_USER.getProvenanceUserId());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
