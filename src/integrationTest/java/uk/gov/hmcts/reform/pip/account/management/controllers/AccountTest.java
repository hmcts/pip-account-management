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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTest;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSubscriber;

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
@SuppressWarnings({"PMD.TooManyMethods"})
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

    private static final String AZURE_URL = "/account/add";
    private static final String PI_URL = "/account/add/pi";
    private static final String GET_PROVENANCE_USER_URL = "/account/provenance/";
    private static final String EMAIL = "a@b";
    private static final String INVALID_EMAIL = "ab";
    private static final String FIRST_NAME = "First name";
    private static final String SURNAME = "Surname";
    private static final String TITLE = "Title";
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final Roles ROLE = Roles.INTERNAL_ADMIN_CTSC;
    private static final String ISSUER_EMAIL = "issuer@email.com";
    private static final String ISSUER_HEADER = "x-issuer-email";

    private static final String ID = "1234";

    private static final String EMAIL_VALIDATION_MESSAGE = "Invalid email provided. Email must contain an @ symbol";
    private static final String INVALID_NAME_MESSAGE = "Invalid name provided. You must either provide no name, "
        + "or any of the following variations "
        + "1) Title, Firstname and Surname 2) Firstname 3) Title and Surname";
    private static final String DIRECTORY_ERROR = "Error when persisting subscriber into Azure. "
        + "Check that the user doesn't already exist in the directory";

    private static final String TEST_MESSAGE_ID = "Subscriber ID added to subscriber";
    private static final String TEST_MESSAGE_EMAIL = "Email matches sent subscriber";
    private static final String TEST_MESSAGE_FIRST_NAME = "Firstname matches sent subscriber";
    private static final String TEST_MESSAGE_SURNAME = "Surname matches sent subscriber";
    private static final String TEST_MESSAGE_TITLE = "Title matches sents subscriber";
    private static final String ERROR_RESPONSE_USER_PROVENANCE = "No user found with the provenanceUserId: 1234";

    private ObjectMapper objectMapper;

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
    }

    @AfterEach
    public void reset() {
        Mockito.reset(graphClient, userCollectionRequest, userCollectionRequestBuilder);
    }

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    void creationOfValidSubscriber() throws Exception {

        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        subscriber.setSurname(SURNAME);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(subscriber)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();


        ConcurrentHashMap<CreationEnum, List<Subscriber>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(0, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "No errored account should be returned");
        assertEquals(1, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Created account should be returned");

        Subscriber returnedSubscriber = subscribers.get(CreationEnum.CREATED_ACCOUNTS).get(0);

        assertEquals(ID, returnedSubscriber.getAzureSubscriberId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, returnedSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, returnedSubscriber.getTitle(), TEST_MESSAGE_TITLE);
    }

    @Test
    void testCreationOfInvalidEmailSubscriber() throws Exception {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail("ab");
        subscriber.setSurname(SURNAME);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(subscriber)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 errored account should be returned");
        assertEquals(0, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "0 created accounts should be returned");

        List<Object> subscribersList = subscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredSubscriber erroredSubscriber = objectMapper.convertValue(subscribersList.get(0),
                                                                     ErroredSubscriber.class);

        assertNull(erroredSubscriber.getAzureSubscriberId(), TEST_MESSAGE_ID);
        assertEquals("ab", erroredSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, erroredSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, erroredSubscriber.getErrorMessages().get(0),
                   "Error message is displayed for an invalid email");

    }

    @Test
    void testCreationOfNoEmailSubscriber() throws Exception {
        Subscriber subscriber = new Subscriber();
        subscriber.setSurname(SURNAME);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(subscriber)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 errored account should be returned");
        assertEquals(0, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "0 created accounts should be returned");

        List<Object> subscribersList = subscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredSubscriber erroredSubscriber = objectMapper.convertValue(subscribersList.get(0),
                                                                        ErroredSubscriber.class);

        assertNull(erroredSubscriber.getAzureSubscriberId(),  TEST_MESSAGE_ID);
        assertNull(erroredSubscriber.getEmail(), "Email has not been sent");
        assertEquals(FIRST_NAME, erroredSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, erroredSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, erroredSubscriber.getErrorMessages().get(0),
                     "Error message is displayed for an invalid email");
    }

    @Test
    void testCreationOfInvalidNameSubscriber() throws Exception {
        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(subscriber)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 errored account should be returned");
        assertEquals(0, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "0 created accounts should be returned");

        List<Object> subscribersList = subscribers.get(CreationEnum.ERRORED_ACCOUNTS);
        ErroredSubscriber erroredSubscriber = objectMapper.convertValue(subscribersList.get(0),
                                                                        ErroredSubscriber.class);

        assertNull(erroredSubscriber.getAzureSubscriberId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, erroredSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertNull(erroredSubscriber.getSurname(), "Surname has not been sent");
        assertEquals(TITLE, erroredSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(INVALID_NAME_MESSAGE, erroredSubscriber.getErrorMessages().get(0),
                     "Error message is displayed for an invalid name");
    }

    @Test
    void testCreationOfDuplicateSubscriber() throws Exception {

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenThrow(graphServiceException);

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        subscriber.setSurname(SURNAME);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(subscriber)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(), "1 errored account returned");
        assertEquals(0, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(), "0 created account returned");

        ErroredSubscriber erroredSubscriber = objectMapper.convertValue(
            subscribers.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredSubscriber.class);

        assertNull(erroredSubscriber.getAzureSubscriberId(), "Errored account does not have ID");
        assertEquals(EMAIL, erroredSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, erroredSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(DIRECTORY_ERROR, erroredSubscriber.getErrorMessages().get(0),
                     "Error message matches directory message");
    }

    @Test
    void testBadRequestWhenCreatingAUserWithADuplicateEmailKey() throws Exception {
        String duplicateKeyString = "[{\"email\": \"a@b.com\", \"email\": \"a@b.com\"}]";

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(duplicateKeyString)
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

        Subscriber validSubscriber = new Subscriber();
        validSubscriber.setEmail(EMAIL);
        validSubscriber.setSurname(SURNAME);
        validSubscriber.setFirstName(FIRST_NAME);
        validSubscriber.setTitle(TITLE);

        Subscriber invalidSubscriber = new Subscriber();
        invalidSubscriber.setEmail("abc.test");
        invalidSubscriber.setSurname(SURNAME);
        invalidSubscriber.setFirstName(FIRST_NAME);
        invalidSubscriber.setTitle(TITLE);

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content(objectMapper.writeValueAsString(List.of(validSubscriber, invalidSubscriber)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        ConcurrentHashMap<CreationEnum, List<Object>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 Errored account should be returned");
        assertEquals(1, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Created account should be returned");

        Subscriber returnedValidSubscriber = objectMapper.convertValue(
            subscribers.get(CreationEnum.CREATED_ACCOUNTS).get(0), Subscriber.class);

        assertEquals(ID, returnedValidSubscriber.getAzureSubscriberId(), TEST_MESSAGE_ID);
        assertEquals(EMAIL, returnedValidSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedValidSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedValidSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, returnedValidSubscriber.getTitle(), TEST_MESSAGE_TITLE);

        ErroredSubscriber returnedInvalidSubscriber = objectMapper.convertValue(
            subscribers.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredSubscriber.class);

        assertNull(returnedInvalidSubscriber.getAzureSubscriberId(), "Subscriber ID should be null");
        assertEquals("abc.test", returnedInvalidSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedInvalidSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedInvalidSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, returnedInvalidSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, returnedInvalidSubscriber.getErrorMessages().get(0),
                     "Error message is displayed for an invalid email");
    }

    @Test
    void testCreateSingleUser() throws Exception {
        PiUser validUser = createUser(true, UUID.randomUUID().toString());

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
        PiUser validUser = createUser(true, UUID.randomUUID().toString());
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
        PiUser validUser = createUser(true, UUID.randomUUID().toString());

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

}
