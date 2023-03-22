package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import com.microsoft.graph.requests.UserRequest;
import com.microsoft.graph.requests.UserRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.Request;
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
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ExcessiveImports", "PMD.JUnitTestsShouldIncludeAssert"})
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

    @Autowired
    GraphServiceException graphServiceException;

    private static final String ROOT_URL = "/account";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String CREATE_MEDIA_USER_URL = "/application";
    private static final String GET_PROVENANCE_USER_URL = ROOT_URL + "/provenance/";
    private static final String UPDATE_ACCOUNT_URL = ROOT_URL + "/provenance/";
    private static final String EMAIL_URL = ROOT_URL + "/emails";

    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String INVALID_EMAIL = "ab";
    private static final String SURNAME = "Surname";
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final Roles ROLE = Roles.INTERNAL_ADMIN_CTSC;
    private static final String ISSUER_ID = "1234-1234-1234-1234";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String ADMIN_HEADER = "x-admin-id";
    private static final String GIVEN_NAME = "Given Name";
    private static final String ID = "1234";
    private static final String ADDITIONAL_ID = "4321";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private static final String ERROR_RESPONSE_USER_PROVENANCE = "No user found with provenance user ID: 1234";
    private static final String NOT_FOUND_STATUS_CODE_MESSAGE = "Status code does not match not found";
    private static final String TEST_UUID_STRING = UUID.randomUUID().toString();
    private static final String USER_SHOULD_MATCH = "Users should match";
    private static final String DELETE_PATH = "/delete/";
    private static final String UPDATE_PATH = "/update/";
    private static final String REPLACE_STRING = "%s/%s/%s";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private PiUser validUser;

    private PiUser createUser(boolean valid, String id) {
        PiUser user = new PiUser();
        user.setEmail(valid ? EMAIL : INVALID_EMAIL);
        user.setProvenanceUserId(id);
        user.setUserProvenance(PROVENANCE);
        user.setRoles(ROLE);
        user.setForenames(GIVEN_NAME);
        user.setSurname(SURNAME);

        return user;
    }

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void setup() {
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
    void testCreateSingleUser() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 User should be created"
        );
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
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser1, validUser2)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(2, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 Users should be created"
        );
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
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser1, validUser2)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(2, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "2 Users should be created"
        );
    }

    @Test
    void testCreateSingleErroredUser() throws Exception {
        PiUser invalidUser = createUser(false, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(invalidUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 User should be errored"
        );
    }

    @Test
    void testCreateMultipleErroredUsers() throws Exception {
        PiUser invalidUser1 = createUser(false, UUID.randomUUID().toString());
        PiUser invalidUser2 = createUser(false, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(invalidUser1, invalidUser2)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(2, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "2 Users should be errored"
        );
    }

    @Test
    void testCreateMultipleUsersCreateAndErrored() throws Exception {
        PiUser invalidUser = createUser(false, UUID.randomUUID().toString());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser, invalidUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                response.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        assertEquals(1, mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).size(),
                     "1 User should be created"
        );
        assertEquals(1, mappedResponse.get(CreationEnum.ERRORED_ACCOUNTS).size(),
                     "1 User should be errored"
        );
    }

    @Test
    void testGetUserByProvenanceIdReturnsUser() throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(String.format(REPLACE_STRING, GET_PROVENANCE_USER_URL, validUser.getUserProvenance(),
                               validUser.getProvenanceUserId()
            ))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response = mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();
        PiUser returnedUser = OBJECT_MAPPER.readValue(
            response.getResponse().getContentAsString(),
            PiUser.class
        );
        assertEquals(validUser.getProvenanceUserId(), returnedUser.getProvenanceUserId(),
                     USER_SHOULD_MATCH
        );
        assertThat(returnedUser.getCreatedDate()).as("Created date must not be null").isNotNull();
    }

    @Test
    void testGetUserByProvenanceIdReturnsNotFound() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(String.format(REPLACE_STRING, GET_PROVENANCE_USER_URL, UserProvenances.CFT_IDAM, ID))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult response =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isNotFound()).andReturn();
        assertEquals(404, response.getResponse().getStatus(), "Status codes should match");
        assertTrue(
            response.getResponse().getContentAsString().contains(ERROR_RESPONSE_USER_PROVENANCE),
            "Should contain error message"
        );
    }

    @Test
    void testGetUserEmailsByIds() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(EMAIL_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(TEST_UUID_STRING)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isOk()).andReturn();

        assertEquals(OK.value(), mvcResult.getResponse().getStatus(),
                     "Status codes does match OK"
        );
    }

    @Test
    void testUpdateAccountLastVerifiedDateSuccessful() throws Exception {
        MockHttpServletRequestBuilder setupRequest = MockMvcRequestBuilders
            .post(PI_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                     + validUser.getProvenanceUserId())
            .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
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
            .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                     + validUser.getProvenanceUserId())
            .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
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
            .content(OBJECT_MAPPER.writeValueAsString(Map.of(
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
            .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(setupRequest).andExpect(status().isCreated());

        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                     + validUser.getProvenanceUserId())
            .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
                "email", "test@test.com")))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("The field 'email' could not be updated")));
    }

    @Test
    void testGetUserById() throws Exception {
        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder getRequest = MockMvcRequestBuilders
            .get(ROOT_URL + "/" + createdUserId);

        MvcResult responseGetUser = mockMvc.perform(getRequest).andExpect(status().isOk()).andReturn();

        PiUser returnedUser = OBJECT_MAPPER.readValue(
            responseGetUser.getResponse().getContentAsString(),
            PiUser.class
        );
        assertEquals(createdUserId, returnedUser.getUserId().toString(), USER_SHOULD_MATCH);
    }

    @Test
    void testGetUserByIdNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ROOT_URL + "/" + UUID.randomUUID());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isNotFound()).andReturn();

        assertEquals(NOT_FOUND.value(), mvcResult.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE_MESSAGE
        );
    }

    @Test
    void testDeleteAccount() throws Exception {
        validUser.setUserProvenance(UserProvenances.CFT_IDAM);
        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder deleteRequest = MockMvcRequestBuilders
            .delete(ROOT_URL + DELETE_PATH + createdUserId);

        MvcResult mvcResult = mockMvc.perform(deleteRequest).andExpect(status().isOk()).andReturn();
        assertEquals("User deleted", mvcResult.getResponse().getContentAsString(),
                     "Failed to delete user"
        );
    }

    @Test
    void testDeleteAccountNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(ROOT_URL + DELETE_PATH + UUID.randomUUID());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isNotFound()).andReturn();

        assertEquals(NOT_FOUND.value(), mvcResult.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE_MESSAGE
        );
    }

    @Test
    void testUpdateUpdateAccountRoleById() throws Exception {
        validUser.setUserProvenance(UserProvenances.CFT_IDAM);
        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
            .put(ROOT_URL + UPDATE_PATH + createdUserId + "/" + Roles.INTERNAL_ADMIN_LOCAL);

        MvcResult responseUpdatedUser = mockMvc.perform(updateRequest)
            .andExpect(status().isOk()).andReturn();

        assertEquals(
            "User with ID " + createdUserId + " has been updated to a " + Roles.INTERNAL_ADMIN_LOCAL,
            responseUpdatedUser.getResponse().getContentAsString(), "Failed to update account"
        );
    }

    @Test
    void testUpdateUpdateAccountRoleByIdWithAdminProvided() throws Exception {
        validUser.setUserProvenance(UserProvenances.CFT_IDAM);
        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
            .put(ROOT_URL + UPDATE_PATH + createdUserId + "/" + Roles.INTERNAL_ADMIN_LOCAL)
            .header(ADMIN_HEADER, UUID.randomUUID());

        MvcResult responseUpdatedUser = mockMvc.perform(updateRequest)
            .andExpect(status().isOk()).andReturn();

        assertEquals(
            "User with ID " + createdUserId + " has been updated to a " + Roles.INTERNAL_ADMIN_LOCAL,
            responseUpdatedUser.getResponse().getContentAsString(), "Failed to update account"
        );
    }

    @Test
    void testUpdateUpdateAccountRoleByIdWithSameAdminId() throws Exception {
        validUser.setUserProvenance(UserProvenances.CFT_IDAM);
        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(PI_URL)
                .content(OBJECT_MAPPER.writeValueAsString(List.of(validUser)))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateUser = mockMvc.perform(createRequest)
            .andExpect(status().isCreated()).andReturn();
        ConcurrentHashMap<CreationEnum, List<Object>> mappedResponse =
            OBJECT_MAPPER.readValue(
                responseCreateUser.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

        String createdUserId = mappedResponse.get(CreationEnum.CREATED_ACCOUNTS).get(0).toString();

        MockHttpServletRequestBuilder updateRequest = MockMvcRequestBuilders
            .put(ROOT_URL + UPDATE_PATH + createdUserId + "/" + Roles.INTERNAL_ADMIN_LOCAL)
            .header(ADMIN_HEADER, createdUserId);

        MvcResult responseUpdatedUser = mockMvc.perform(updateRequest)
            .andExpect(status().isForbidden()).andReturn();

        assertTrue(responseUpdatedUser.getResponse().getContentAsString().contains(
            "User with id " + createdUserId + " is unable to update user ID " + createdUserId),
                   "Failed to update account"
        );
    }

    @Test
    void testUpdateAccountRoleByIdNotFound() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(ROOT_URL + UPDATE_PATH + UUID.randomUUID() + "/" + Roles.INTERNAL_ADMIN_LOCAL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isNotFound()).andReturn();

        assertEquals(NOT_FOUND.value(), mvcResult.getResponse().getStatus(),
                     NOT_FOUND_STATUS_CODE_MESSAGE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedCreateUsers() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
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
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetUserByProvenanceId() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(String.format("%s/%s/%s", GET_PROVENANCE_USER_URL, UserProvenances.CFT_IDAM, ID))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedCheckUserAuthorised() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format("%s/isAuthorised/%s/%s/%s", ROOT_URL, UUID.randomUUID(),
                               ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC
            ));

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetUserEmailsByIds() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(EMAIL_URL)
            .content(OBJECT_MAPPER.writeValueAsString(List.of(TEST_UUID_STRING)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedUpdateAccount() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                     + validUser.getProvenanceUserId())
            .content(OBJECT_MAPPER.writeValueAsString(Collections.singletonMap(
                "email", "test@test.com")))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedGetUserById() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ROOT_URL + "/" + UUID.randomUUID());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDeleteAccount() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(ROOT_URL + "/delete/" + UUID.randomUUID());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedUpdateAccountById() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(ROOT_URL + "/update/" + UUID.randomUUID() + "/" + Roles.INTERNAL_ADMIN_LOCAL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
