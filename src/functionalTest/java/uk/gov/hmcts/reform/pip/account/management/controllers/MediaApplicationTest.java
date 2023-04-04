package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationDto;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})

@SuppressWarnings({"PMD.TooManyMethods", "PMD.LawOfDemeter", "PMD.JUnitTestsShouldIncludeAssert"})
class MediaApplicationTest {

    @Autowired
    BlobContainerClient blobContainerClient;

    @Autowired
    BlobClient blobClient;

    @Autowired
    private MockMvc mockMvc;

    private static final String ROOT_URL = "/application";
    private static final String GET_STATUS_URL = ROOT_URL + "/status/{status}";
    private static final String PUT_URL = ROOT_URL + "/{id}/{status}";
    private static final String UPDATE_APPLICATION_REJECTION_URL = ROOT_URL + "/{id}/{status}/reasons";
    private static final String DELETE_URL = ROOT_URL + "/{id}";
    private static final String GET_BY_ID_URL = ROOT_URL + "/{id}";
    private static final String GET_IMAGE_BY_ID_URL = ROOT_URL + "/image/{id}";
    private static final String REPORT_APPLICATIONS_URL = ROOT_URL + "/reporting";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_username";
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.role";

    private ObjectMapper objectMapper;
    private static final String FULL_NAME = "Test user";
    private static final String FORMATTED_FULL_NAME = "Test user";
    private static final String EMAIL = "test@justice.gov.uk";
    private static final String EMPLOYER = "Test employer";
    private static final String BLOB_IMAGE_URL = "https://localhost";
    private static final MediaApplicationStatus STATUS = MediaApplicationStatus.PENDING;
    private static final MediaApplicationStatus UPDATED_STATUS = MediaApplicationStatus.APPROVED;
    private static final UUID TEST_ID = UUID.randomUUID();
    private static final String PENDING_STATUS = "PENDING";

    private static final String NOT_FOUND_ERROR = "Returned ID does not match the expected ID";
    private static final String EMAIL_NOT_MATCH = "Emails do not match";
    private static final String FULLNAME_NOT_FORMATTTED = "Full name hasn't been formatted";
    private static final String STATUSES_NOT_MATCH = "Statuses do not match";
    private static final String ERROR_MESSAGE_MISMATCH = "Error messages do not match";

    private static final Map<String, List<String>> REASONS = new ConcurrentHashMap<>();

    @BeforeAll
    static void beforeAllSetup() {
        REASONS.put("Reason A", List.of("Text A", "Text B"));
    }

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
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

            MvcResult mvcResult = mockMvc.perform(multipart(ROOT_URL)
                                                      .file(imageFile)
                                                      .flashAttr("application", applicationDto)
                                                      .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isOk()).andReturn();

            return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MediaApplication.class);
        }
    }

    private MvcResult createApplicationRequest(String fullname, String email,
                                               String employer, String status) throws Exception {
        MediaApplicationDto applicationDto = new MediaApplicationDto();
        applicationDto.setFullName(fullname);
        applicationDto.setEmail(email);
        applicationDto.setEmployer(employer);
        applicationDto.setStatus(status);

        try (InputStream imageInputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("files/test-image.png")) {

            MockMultipartFile imageFile = new MockMultipartFile("file", "test-image.png",
                                                                "", imageInputStream
            );


            when(blobContainerClient.getBlobClient(any())).thenReturn(blobClient);
            when(blobContainerClient.getBlobContainerUrl()).thenReturn(BLOB_IMAGE_URL);

            return mockMvc.perform(multipart(ROOT_URL)
                                       .file(imageFile)
                                       .flashAttr("application", applicationDto)
                                       .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andReturn();
        }
    }

    @Test
    void testCreateApplicationInvalidEmail() throws Exception {
        MvcResult mvcResult = createApplicationRequest(
            FULL_NAME,
            "email...@justice.gov.uk",
            EMPLOYER,
            PENDING_STATUS
        );
        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), STATUSES_NOT_MATCH);
        assertEquals(
            "{\"email\":\"must be a well-formed email address\"}",
            mvcResult.getResponse().getContentAsString(),
            ERROR_MESSAGE_MISMATCH
        );
    }

    @Test
    void testCreateApplicationEmptyEmail() throws Exception {
        MvcResult mvcResult = createApplicationRequest(FULL_NAME, "", EMPLOYER, PENDING_STATUS);
        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), STATUSES_NOT_MATCH);
        assertEquals(
            "{\"email\":\"email shouldn't be blank or null\"}",
            mvcResult.getResponse().getContentAsString(),
            ERROR_MESSAGE_MISMATCH
        );
    }

    @Test
    void testCreateApplicationEmptyEmployer() throws Exception {
        MvcResult mvcResult = createApplicationRequest(FULL_NAME, EMAIL, "", PENDING_STATUS);
        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), STATUSES_NOT_MATCH);
        assertEquals(
            "{\"employer\":\"employer shouldn't be blank or null\"}",
            mvcResult.getResponse().getContentAsString(),
            ERROR_MESSAGE_MISMATCH
        );
    }

    @Test
    void testCreateApplicationEmptyName() throws Exception {
        MvcResult mvcResult = createApplicationRequest("", EMAIL, EMPLOYER, PENDING_STATUS);
        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), STATUSES_NOT_MATCH);
        assertEquals(
            "{\"fullName\":\"fullName shouldn't be blank or null\"}",
            mvcResult.getResponse().getContentAsString(),
            ERROR_MESSAGE_MISMATCH
        );
    }

    @Test
    void testCreateApplicationEmptyStatus() throws Exception {
        MvcResult mvcResult = createApplicationRequest(FULL_NAME, EMAIL, EMPLOYER, "");
        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), STATUSES_NOT_MATCH);
        assertEquals(
            "{\"status\":\"status should be one of PENDING, REJECTED or APPROVED\"}",
            mvcResult.getResponse().getContentAsString(),
            ERROR_MESSAGE_MISMATCH
        );
    }

    @Test
    void testCreateApplicationNullStatus() throws Exception {
        MvcResult mvcResult = createApplicationRequest(FULL_NAME, EMAIL, EMPLOYER, null);
        assertEquals(HttpStatus.BAD_REQUEST.value(), mvcResult.getResponse().getStatus(), STATUSES_NOT_MATCH);
        assertEquals(
            "{\"status\":\"status shouldn't be null\"}",
            mvcResult.getResponse().getContentAsString(),
            ERROR_MESSAGE_MISMATCH
        );
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testCreateApplicationUnauthorised() throws Exception {
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
            mockMvc.perform(multipart(ROOT_URL)
                                .file(imageFile)
                                .flashAttr("application", applicationDto)
                                .contentType(MediaType.MULTIPART_FORM_DATA_VALUE))
                .andExpect(status().isForbidden());
        }
    }

    @Test
    void testGetApplications() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication[] arrayApplications = objectMapper.readValue(
            mvcResult.getResponse()
                .getContentAsString(),
            MediaApplication[].class
        );

        List<MediaApplication> applicationList = Arrays.asList(arrayApplications);

        assertEquals(application.getEmail(), applicationList.get(0).getEmail(), EMAIL_NOT_MATCH);
        assertEquals(FORMATTED_FULL_NAME, applicationList.get(0).getFullName(), FULLNAME_NOT_FORMATTTED);
        assertEquals(application.getStatus(), applicationList.get(0).getStatus(), STATUSES_NOT_MATCH);
        assertNotNull(applicationList.get(0).getImage(), "Image url is null");
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testGetApplicationsUnauthorised() throws Exception {
        mockMvc.perform(get(ROOT_URL))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetApplicationsByStatus() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(get(GET_STATUS_URL, PENDING_STATUS))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication[] arrayApplications = objectMapper.readValue(
            mvcResult.getResponse()
                .getContentAsString(),
            MediaApplication[].class
        );

        List<MediaApplication> applicationList = Arrays.asList(arrayApplications);

        assertEquals(application.getEmail(), applicationList.get(0).getEmail(), EMAIL_NOT_MATCH);
        assertEquals(FORMATTED_FULL_NAME, applicationList.get(0).getFullName(), FULLNAME_NOT_FORMATTTED);
        assertEquals(application.getStatus(), applicationList.get(0).getStatus(), STATUSES_NOT_MATCH);
        assertNotNull(applicationList.get(0).getImage(), "Image url is null");

    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testGetApplicationsByStatusUnauthorised() throws Exception {
        mockMvc.perform(get(GET_STATUS_URL, PENDING_STATUS))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetApplicationById() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(get(GET_BY_ID_URL, application.getId()))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication returnedApplication = objectMapper.readValue(
            mvcResult.getResponse()
                .getContentAsString(),
            MediaApplication.class
        );

        assertEquals(application.getEmail(), returnedApplication.getEmail(), EMAIL_NOT_MATCH);
        assertEquals(FORMATTED_FULL_NAME, returnedApplication.getFullName(), FULLNAME_NOT_FORMATTTED);
        assertEquals(application.getStatus(), returnedApplication.getStatus(), STATUSES_NOT_MATCH);
    }

    @Test
    void testGetApplicationByIdNotFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(GET_BY_ID_URL, TEST_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().contains(String.valueOf(TEST_ID)), NOT_FOUND_ERROR);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testGetApplicationByIdUnauthorised() throws Exception {
        mockMvc.perform(get(GET_BY_ID_URL, TEST_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void testGetImageById() throws Exception {
        MediaApplication application = createApplication();

        final byte[] data = "Image".getBytes(StandardCharsets.UTF_8);
        BinaryData binaryData = BinaryData.fromBytes(data);

        when(blobClient.downloadContent()).thenReturn(binaryData);

        MvcResult mvcResult = mockMvc.perform(get(GET_IMAGE_BY_ID_URL, application.getImage()))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("Image", mvcResult.getResponse().getContentAsString(), "Images do not match");
    }

    @Test
    void testGetImageByIdNotFound() throws Exception {
        createApplication();

        when(blobClient.downloadContent()).thenThrow(BlobStorageException.class);

        MvcResult mvcResult = mockMvc.perform(get(GET_IMAGE_BY_ID_URL, TEST_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().contains(String.valueOf(TEST_ID)), NOT_FOUND_ERROR);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testGetImageByIdUnauthorised() throws Exception {
        mockMvc.perform(get(GET_IMAGE_BY_ID_URL, TEST_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateApplication() throws Exception {
        MediaApplication application = createApplication();

        assertEquals(STATUS, application.getStatus(), "Original statuses do not match");

        MvcResult mvcResult = mockMvc.perform(put(PUT_URL, application.getId(), UPDATED_STATUS))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication returnedApplication = objectMapper.readValue(
            mvcResult.getResponse()
                .getContentAsString(),
            MediaApplication.class
        );

        assertEquals(UPDATED_STATUS, returnedApplication.getStatus(), "Updated statuses do not match");

    }

    @Test
    void testUpdateApplicationNotFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(put(PUT_URL, TEST_ID, STATUS))
            .andExpect(status().isNotFound())
            .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().contains(String.valueOf(TEST_ID)), NOT_FOUND_ERROR);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUpdateApplicationUnauthorised() throws Exception {
        mockMvc.perform(put(PUT_URL, TEST_ID, STATUS))
            .andExpect(status().isForbidden());
    }

    @Test
    void testUpdateApplicationRejection() throws Exception {
        MediaApplication application = createApplication();

        assertEquals(STATUS, application.getStatus(), "Original statuses do not match");

        MvcResult mvcResult = mockMvc.perform(put(UPDATE_APPLICATION_REJECTION_URL, application.getId(),
                                                  MediaApplicationStatus.REJECTED
            ).content(objectMapper.writeValueAsString(REASONS)).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication returnedApplication = objectMapper.readValue(
            mvcResult.getResponse()
                .getContentAsString(),
            MediaApplication.class
        );

        assertEquals(MediaApplicationStatus.REJECTED, returnedApplication.getStatus(),
                     "Updated statuses do not match"
        );
    }

    @Test
    void testUpdateApplicationRejectionNotFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(put(UPDATE_APPLICATION_REJECTION_URL, TEST_ID,
                                                  MediaApplicationStatus.REJECTED
            ).content(objectMapper.writeValueAsString(REASONS)).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString()
                       .contains("Application with id " + TEST_ID + " could not be found"), NOT_FOUND_ERROR);

    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUpdateApplicationRejectionUnauthorised() throws Exception {
        mockMvc.perform(put(UPDATE_APPLICATION_REJECTION_URL, TEST_ID,
                            MediaApplicationStatus.REJECTED
            ).content(objectMapper.writeValueAsString(REASONS)).contentType(MediaType.APPLICATION_JSON)
                            .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    void testDeleteApplication() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(delete(DELETE_URL, application.getId()))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("Application deleted", mvcResult.getResponse().getContentAsString(),
                     "Application was not deleted"
        );
    }

    @Test
    void testDeleteApplicationNotFound() throws Exception {

        MvcResult mvcResult = mockMvc.perform(delete(DELETE_URL, TEST_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().contains(String.valueOf(TEST_ID)), NOT_FOUND_ERROR);
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testDeleteApplicationUnauthorised() throws Exception {
        mockMvc.perform(delete(DELETE_URL, TEST_ID))
            .andExpect(status().isForbidden());
    }

    @Test
    void testReportApplicationsSuccess() throws Exception {
        mockMvc.perform(post(REPORT_APPLICATIONS_URL))
            .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testReportApplicationsUnauthorised() throws Exception {
        mockMvc.perform(post(REPORT_APPLICATIONS_URL))
            .andExpect(status().isForbidden());
    }
}
