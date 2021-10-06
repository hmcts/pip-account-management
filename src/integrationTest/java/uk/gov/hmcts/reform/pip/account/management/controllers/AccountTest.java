package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import okhttp3.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
import uk.gov.hmcts.reform.pip.account.management.model.Subscriber;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

        assertEquals(ID, returnedSubscriber.getAzureSubscriberId(), "Subscriber ID added to subscriber");
        assertNotNull(returnedSubscriber.getTableSubscriberId(), "Table subscriber ID should be populated");
        assertEquals(EMAIL, returnedSubscriber.getEmail(), "Email matches sent subscriber");
        assertEquals(FIRST_NAME, returnedSubscriber.getFirstName(), "Firstname matches sent subscriber");
        assertEquals(SURNAME, returnedSubscriber.getSurname(), "Surname matches sent subscriber");
        assertEquals(TITLE, returnedSubscriber.getTitle(), "Title matches sent subscriber");
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
            .andExpect(status().isBadRequest()).andReturn();


        ExceptionResponse exceptionResponse = objectMapper
            .readValue(response.getResponse().getContentAsString(), ExceptionResponse.class);
        assertTrue(exceptionResponse.getMessage().contains("Invalid email"), "Email marked as invalid");
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
            .andExpect(status().isBadRequest()).andReturn();


        ExceptionResponse exceptionResponse = objectMapper
            .readValue(response.getResponse().getContentAsString(), ExceptionResponse.class);
        assertTrue(exceptionResponse.getMessage().contains("Invalid email"), "Email marked as invalid");
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
            .andExpect(status().isBadRequest()).andReturn();

        ExceptionResponse exceptionResponse = objectMapper
            .readValue(response.getResponse().getContentAsString(), ExceptionResponse.class);
        assertTrue(exceptionResponse.getMessage().contains("Invalid name"), "Name marked as invalid");
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


        ConcurrentHashMap<CreationEnum, List<Subscriber>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(), "1 errored account returned");
        assertEquals(0, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(), "0 created account returned");

        Subscriber returnedSubscriber = subscribers.get(CreationEnum.ERRORED_ACCOUNTS).get(0);

        assertNull(returnedSubscriber.getAzureSubscriberId(), "Errored account does not have ID");
        assertNull(returnedSubscriber.getTableSubscriberId(), "Errored accoun");
        assertEquals(EMAIL, returnedSubscriber.getEmail(), "Email matches sent subscriber");
        assertEquals(FIRST_NAME, returnedSubscriber.getFirstName(), "Firstname matches sent subscriber");
        assertEquals(SURNAME, returnedSubscriber.getSurname(), "Surname matches sent subscriber");
        assertEquals(TITLE, returnedSubscriber.getTitle(), "Title matches sent subscriber");
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


        ConcurrentHashMap<CreationEnum, List<Subscriber>> subscribers =
            objectMapper.readValue(response.getResponse().getContentAsString(),
                                   new TypeReference<>() {});

        assertEquals(1, subscribers.get(CreationEnum.ERRORED_ACCOUNTS).size(), "1 errored account returned");
        assertEquals(0, subscribers.get(CreationEnum.CREATED_ACCOUNTS).size(), "0 created account returned");

        Subscriber returnedSubscriber = subscribers.get(CreationEnum.ERRORED_ACCOUNTS).get(0);

        assertEquals(ID, returnedSubscriber.getAzureSubscriberId(), "Subscriber ID added to subscriber");
        assertNull(returnedSubscriber.getTableSubscriberId(), "Table ID should be null");
        assertEquals(EMAIL, returnedSubscriber.getEmail(), "Email matches sent subscriber");
        assertEquals(FIRST_NAME, returnedSubscriber.getFirstName(), "Firstname matches sent subscriber");
        assertEquals(SURNAME, returnedSubscriber.getSurname(), "Surname matches sent subscriber");
        assertEquals(TITLE, returnedSubscriber.getTitle(), "Title matches sent subscriber");
    }

}
