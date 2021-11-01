package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.azure.data.tables.models.TableServiceException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
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
import uk.gov.hmcts.reform.pip.account.management.model.ErroredSubscriber;
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTest.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "test")
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

    @Autowired
    TableClient tableClient;

    @Autowired
    PagedIterable<TableEntity> tableEntities;

    private static final String URL = "/account/add";
    private static final String EMAIL = "a@b";
    private static final String FIRST_NAME = "First name";
    private static final String SURNAME = "Surname";
    private static final String TITLE = "Title";

    private static final String ID = "1234";

    private static final String EMAIL_VALIDATION_MESSAGE = "Invalid email provided. Email must contain an @ symbol";
    private static final String INVALID_NAME_MESSAGE = "Invalid name provided. You must either provide no name, "
        + "or any of the following variations "
        + "1) Title, Firstname and Surname 2) Firstname 3) Title and Surname";
    private static final String DIRECTORY_ERROR = "Error when persisting subscriber into Azure. "
        + "Check that the user doesn't already exist in the directory";
    private static final String DUPLICATE_USER_IN_TABLE = "A user with this email already exists in the table";
    private static final String FAILURE_TO_CREATE_MESSAGE = "Error while persisting subscriber into the table service";

    private static final String TEST_MESSAGE_ID = "Subscriber ID added to subscriber";
    private static final String TEST_MESSAGE_TABLE_ID = "Table subscriber ID should be populated";
    private static final String TEST_MESSAGE_EMAIL = "Email matches sent subscriber";
    private static final String TEST_MESSAGE_FIRST_NAME = "Firstname matches sent subscriber";
    private static final String TEST_MESSAGE_SURNAME = "Surname matches sent subscriber";
    private static final String TEST_MESSAGE_TITLE = "Title matches sents subscriber";



    @AfterEach
    public void reset() {
        Mockito.reset(graphClient, tableClient, userCollectionRequest, userCollectionRequestBuilder);
    }

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    void creationOfValidSubscriber() throws Exception {

        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.empty());

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        subscriber.setSurname(SURNAME);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        ObjectMapper objectMapper = new ObjectMapper();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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
        assertNotNull(returnedSubscriber.getTableSubscriberId(), TEST_MESSAGE_TABLE_ID);
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

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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
        assertNull(erroredSubscriber.getTableSubscriberId(), TEST_MESSAGE_TABLE_ID);
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

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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
        assertNull(erroredSubscriber.getTableSubscriberId(), TEST_MESSAGE_TABLE_ID);
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

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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
        assertNull(erroredSubscriber.getTableSubscriberId(), TEST_MESSAGE_TABLE_ID);
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

        ObjectMapper objectMapper = new ObjectMapper();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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
        assertNull(erroredSubscriber.getTableSubscriberId(), "Errored account");
        assertEquals(EMAIL, erroredSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, erroredSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(DIRECTORY_ERROR, erroredSubscriber.getErrorMessages().get(0),
                     "Error message matches directory message");
    }

    @Test
    void testCreationOfDuplicateSubscriberInTableService() throws Exception {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.of(new TableEntity("a", "b")));

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        subscriber.setSurname(SURNAME);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        ObjectMapper objectMapper = new ObjectMapper();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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

        assertEquals(ID, erroredSubscriber.getAzureSubscriberId(), TEST_MESSAGE_ID);
        assertNull(erroredSubscriber.getTableSubscriberId(), "Table ID should be null");
        assertEquals(EMAIL, erroredSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, erroredSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(DUPLICATE_USER_IN_TABLE, erroredSubscriber.getErrorMessages().get(0),
                     "Error message matches duplicate user message");
    }

    @Test
    void testErrorWhileCommunicatingWithTableService() throws Exception {
        User userToReturn = new User();
        userToReturn.id = ID;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(userToReturn);

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.empty());
        doThrow(new TableServiceException("Exception communicating with Azure", null))
            .when(tableClient).createEntity(any());

        Subscriber subscriber = new Subscriber();
        subscriber.setEmail(EMAIL);
        subscriber.setSurname(SURNAME);
        subscriber.setFirstName(FIRST_NAME);
        subscriber.setTitle(TITLE);

        ObjectMapper objectMapper = new ObjectMapper();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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

        assertEquals(ID, erroredSubscriber.getAzureSubscriberId(), TEST_MESSAGE_ID);
        assertNull(erroredSubscriber.getTableSubscriberId(), "Table ID should be null");
        assertEquals(EMAIL, erroredSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, erroredSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, erroredSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, erroredSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(FAILURE_TO_CREATE_MESSAGE, erroredSubscriber.getErrorMessages().get(0),
                     "Error message matches failure to create message");
    }

    @Test
    void testBadRequestWhenCreatingAUserWithADuplicateEmailKey() throws Exception {
        String duplicateKeyString = "[{\"email\": \"a@b.com\", \"email\": \"a@b.com\"}]";

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
            .content(duplicateKeyString)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest()).andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
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

        when(tableClient.listEntities(any(), any(), any())).thenReturn(tableEntities);
        when(tableEntities.stream()).thenReturn(Stream.empty());

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

        ObjectMapper objectMapper = new ObjectMapper();

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(URL)
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
        assertNotNull(returnedValidSubscriber.getTableSubscriberId(), TEST_MESSAGE_TABLE_ID);
        assertEquals(EMAIL, returnedValidSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedValidSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedValidSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, returnedValidSubscriber.getTitle(), TEST_MESSAGE_TITLE);

        ErroredSubscriber returnedInvalidSubscriber = objectMapper.convertValue(
            subscribers.get(CreationEnum.ERRORED_ACCOUNTS).get(0), ErroredSubscriber.class);

        assertNull(returnedInvalidSubscriber.getAzureSubscriberId(), "Subscriber ID should be null");
        assertNull(returnedInvalidSubscriber.getTableSubscriberId(), "Table ID should be null");
        assertEquals("abc.test", returnedInvalidSubscriber.getEmail(), TEST_MESSAGE_EMAIL);
        assertEquals(FIRST_NAME, returnedInvalidSubscriber.getFirstName(), TEST_MESSAGE_FIRST_NAME);
        assertEquals(SURNAME, returnedInvalidSubscriber.getSurname(), TEST_MESSAGE_SURNAME);
        assertEquals(TITLE, returnedInvalidSubscriber.getTitle(), TEST_MESSAGE_TITLE);
        assertEquals(EMAIL_VALIDATION_MESSAGE, returnedInvalidSubscriber.getErrorMessages().get(0),
                     "Error message is displayed for an invalid email");
    }

}
