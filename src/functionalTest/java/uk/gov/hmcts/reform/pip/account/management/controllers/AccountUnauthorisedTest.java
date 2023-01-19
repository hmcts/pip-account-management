package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
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
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.ListType;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.Sensitivity;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "unauthroized_user", authorities = {"APPROLE_unknown.user"})

@SuppressWarnings({"PMD.TooManyMethods", "PMD.LawOfDemeter", "PMD.ExcessiveImports",
    "PMD.JUnitTestsShouldIncludeAssert", "PMD.ExcessiveClassLength"})
class AccountUnauthorisedTest {

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

    @Autowired
    GraphServiceException graphServiceException;

    private static final String ROOT_URL = "/account";
    private static final String ADMIN_ROOT_URL = "/account/admin/";
    private static final String AZURE_URL = ROOT_URL + "/add/azure";
    private static final String PI_URL = ROOT_URL + "/add/pi";
    private static final String GET_PROVENANCE_USER_URL = ROOT_URL + "/provenance/";
    private static final String UPDATE_ACCOUNT_URL = ROOT_URL + "/provenance/";
    private static final String NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive/notify";
    private static final String DELETE_EXPIRED_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive";
    private static final String NOTIFY_INACTIVE_ADMIN_ACCOUNTS_URL = ROOT_URL + "/admin/inactive/notify";
    private static final String DELETE_EXPIRED_ADMIN_ACCOUNTS_URL = ROOT_URL + "/admin/inactive";
    private static final String NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive/notify";
    private static final String DELETE_EXPIRED_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive";
    private static final String GET_ALL_ACCOUNTS_EXCEPT_THIRD_PARTY = ROOT_URL + "/all";
    private static final String CREATE_SYSTEM_ADMIN_URL = ROOT_URL + "/add/system-admin";
    private static final String EMAIL_URL = ROOT_URL + "/emails";

    private static final String EMAIL = "test_account_admin@hmcts.net";
    private static final String INVALID_EMAIL = "ab";
    private static final UserProvenances PROVENANCE = UserProvenances.PI_AAD;
    private static final Roles ROLE = Roles.INTERNAL_ADMIN_CTSC;
    private static final String ISSUER_ID = "1234-1234-1234-1234";

    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String GIVEN_NAME = "Given Name";

    private static final String ID = "1234";
    private static final String ADDITIONAL_ID = "4321";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";
    private static final String TEST_UUID_STRING = UUID.randomUUID().toString();

    private ObjectMapper objectMapper;

    private PiUser validUser;

    private PiUser createUser(boolean valid, String id) {
        PiUser user = new PiUser();
        user.setEmail(valid ? EMAIL : INVALID_EMAIL);
        user.setProvenanceUserId(id);
        user.setUserProvenance(PROVENANCE);
        user.setRoles(ROLE);
        user.setForenames("Forename");
        user.setSurname("Surname");

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
    void testUnauthorizedCreateAccount() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(AZURE_URL)
            .content("[]")
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedCreateUsers() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post(PI_URL)
            .content("[]")
            .header(ISSUER_HEADER, ISSUER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedGetUserByProvenanceId() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(String.format("%s/%s/%s", GET_PROVENANCE_USER_URL, UserProvenances.CFT_IDAM, ID))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedCheckUserAuthorised() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(String.format("%s/isAuthorised/%s/%s/%s", ROOT_URL, UUID.randomUUID(),
                               ListType.SJP_PRESS_LIST, Sensitivity.PUBLIC
            ));

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedGetUserEmailsByIds() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(EMAIL_URL)
            .content(objectMapper.writeValueAsString(List.of(TEST_UUID_STRING)))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedUpdateAccount() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(UPDATE_ACCOUNT_URL + validUser.getUserProvenance() + "/"
                     + validUser.getProvenanceUserId())
            .content(objectMapper.writeValueAsString(Collections.singletonMap(
                "email", "test@test.com")))
            .contentType(MediaType.APPLICATION_JSON);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedNotifyInactiveMediaAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedDeleteExpiredMediaAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_MEDIA_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedNotifyInactiveAdminAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_ADMIN_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedDeleteExpiredAdminAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_ADMIN_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedNotifyInactiveIdamAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedDeleteExpiredIdamAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_IDAM_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedGetAllAccountsExceptThirdParty() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(GET_ALL_ACCOUNTS_EXCEPT_THIRD_PARTY);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedGetAllThirdPartyAccounts() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .get(ROOT_URL + "/all/third-party");

        MvcResult mvcResult =
            mockMvc.perform(mockHttpServletRequestBuilder).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedGetUserById() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ROOT_URL + "/" + UUID.randomUUID());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedDeleteAccount() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(ROOT_URL + "/delete/" + UUID.randomUUID());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedUpdateAccountById() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .put(ROOT_URL + "/update/" + UUID.randomUUID() + "/" + Roles.INTERNAL_ADMIN_LOCAL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedCreateSystemAdminAccount() throws Exception {
        SystemAdminAccount systemAdmin = new SystemAdminAccount();
        systemAdmin.setFirstName("testSysAdminFirstname");
        systemAdmin.setSurname("testSysAdminSurname");
        systemAdmin.setEmail("testSysAdminEmail@justice.gov.uk");

        MockHttpServletRequestBuilder createRequest =
            MockMvcRequestBuilders
                .post(CREATE_SYSTEM_ADMIN_URL)
                .content(objectMapper.writeValueAsString(systemAdmin))
                .header(ISSUER_HEADER, ISSUER_ID)
                .contentType(MediaType.APPLICATION_JSON);

        MvcResult responseCreateSystemAdminUser = mockMvc.perform(createRequest)
            .andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), responseCreateSystemAdminUser.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedGetAdminUserByEmailAndProvenance() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ADMIN_ROOT_URL + validUser.getEmail() + "/" + validUser.getUserProvenance());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testUnauthorizedGetAzureUserInfo() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .get(ROOT_URL + "/azure/" + validUser.getProvenanceUserId());

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(HttpStatus.FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
