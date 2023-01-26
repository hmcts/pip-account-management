package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequest;
import com.microsoft.graph.requests.UserCollectionRequestBuilder;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import okhttp3.Request;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class BulkAccountCreationTest {
    private static final String ROOT_URL = "/account";
    private static final String BULK_UPLOAD = ROOT_URL + "/media-bulk-upload";

    private static final String ISSUER_ID = "1234-1234-1234-1234";
    private static final String ISSUER_HEADER = "x-issuer-id";
    private static final String MEDIA_LIST = "mediaList";
    private static final String GIVEN_NAME = "Given Name";

    private static final String ID = "1234";
    private static final String ADDITIONAL_ID = "4321";
    private static final String MAP_SIZE_MESSAGE = "Map size should match";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GraphServiceClient<Request> graphClient;

    @Autowired
    private UserCollectionRequestBuilder userCollectionRequestBuilder;

    @Autowired
    private UserCollectionRequest userCollectionRequest;

    @Mock
    private UserCollectionPage userCollectionPage;

    @BeforeEach
    void setup() {
        User user = new User();
        user.id = ID;
        user.givenName = GIVEN_NAME;

        User additionalUser = new User();
        additionalUser.id = ADDITIONAL_ID;
        additionalUser.givenName = GIVEN_NAME;

        when(graphClient.users()).thenReturn(userCollectionRequestBuilder);
        when(userCollectionRequestBuilder.buildRequest()).thenReturn(userCollectionRequest);
        when(userCollectionRequest.post(any())).thenReturn(user, additionalUser);
    }

    @AfterEach
    public void reset() {
        Mockito.reset(graphClient, userCollectionRequest, userCollectionRequestBuilder);
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testUploadBulkMedia() throws Exception {
        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/valid.csv")) {
            MockMultipartFile multipartFile = new MockMultipartFile(MEDIA_LIST, IOUtils.toByteArray(inputStream));

            MvcResult mvcResult = mockMvc.perform(multipart(BULK_UPLOAD).file(multipartFile)
                                                      .header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isOk()).andReturn();
            ConcurrentHashMap<CreationEnum, List<?>> users = OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

            assertEquals(2, users.get(CreationEnum.CREATED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
            assertEquals(0, users.get(CreationEnum.ERRORED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
        }
    }

    @Test
    void testUploadBulkMediaFailsCsv() throws Exception {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/invalidCsv.txt")) {
            MockMultipartFile csvFile = new MockMultipartFile(MEDIA_LIST, inputStream);

            MvcResult result = mockMvc.perform(multipart(BULK_UPLOAD).file(csvFile).header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isBadRequest()).andReturn();

            assertTrue(
                result.getResponse().getContentAsString().contains("Failed to parse CSV File"),
                "Should contain error"
            );
        }
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void testUploadBulkMediaEmailOnly() throws Exception {
        userCollectionPage = new UserCollectionPage(new ArrayList<>(), userCollectionRequestBuilder);
        when(userCollectionRequest.filter(any())).thenReturn(userCollectionRequest);
        when(userCollectionRequest.get()).thenReturn(userCollectionPage);

        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/mediaEmailOnly.csv")) {
            MockMultipartFile multipartFile = new MockMultipartFile(MEDIA_LIST, IOUtils.toByteArray(inputStream));

            MvcResult mvcResult = mockMvc.perform(multipart(BULK_UPLOAD).file(multipartFile)
                                                      .header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isOk()).andReturn();
            ConcurrentHashMap<CreationEnum, List<?>> users = OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

            assertEquals(2, users.get(CreationEnum.CREATED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
            assertEquals(0, users.get(CreationEnum.ERRORED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
        }
    }

    @Test
    void testUploadBulkMediaEmailValidation() throws Exception {
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("csv/invalidEmail.csv")) {
            MockMultipartFile multipartFile = new MockMultipartFile(MEDIA_LIST, IOUtils.toByteArray(inputStream));

            MvcResult mvcResult = mockMvc.perform(multipart(BULK_UPLOAD).file(multipartFile)
                                                      .header(ISSUER_HEADER, ISSUER_ID))
                .andExpect(status().isOk()).andReturn();
            ConcurrentHashMap<CreationEnum, List<?>> users = OBJECT_MAPPER.readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
            );

            assertEquals(0, users.get(CreationEnum.CREATED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
            assertEquals(1, users.get(CreationEnum.ERRORED_ACCOUNTS).size(), MAP_SIZE_MESSAGE);
        }
    }
}
