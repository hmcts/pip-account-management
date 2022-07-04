package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTest;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationDto;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTest.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@WithMockUser(username = "admin", authorities = { "APPROLE_api.request.admin" })
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
    private static final String DELETE_URL = ROOT_URL + "/{id}";
    private static final String GET_BY_ID_URL = ROOT_URL + "/{id}";

    private ObjectMapper objectMapper;
    private static final String FULL_NAME = "Test user";
    private static final String FORMATTED_FULL_NAME = "Test user";
    private static final String EMAIL = "test@email.com";
    private static final String EMPLOYER = "Test employer";
    private static final String BLOB_IMAGE_URL = "https://localhost";
    private static final MediaApplicationStatus STATUS = MediaApplicationStatus.PENDING;
    private static final MediaApplicationStatus UPDATED_STATUS = MediaApplicationStatus.APPROVED;
    private static final UUID TEST_ID = UUID.randomUUID();

    private static final String NOT_FOUND_ERROR = "Returned ID does not match the expected ID";

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
        applicationDto.setStatus(STATUS);

        try (InputStream imageInputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("files/test-image.png")) {

            MockMultipartFile imageFile = new MockMultipartFile("file", "test-image.png",
                                                                "", imageInputStream);


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

    @Test
    void testGetApplications() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(get(ROOT_URL))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication[] arrayApplications = objectMapper.readValue(mvcResult.getResponse()
                                                                             .getContentAsString(),
                                                                             MediaApplication[].class);

        List<MediaApplication> applicationList = Arrays.asList(arrayApplications);

        assertEquals(application.getEmail(), applicationList.get(0).getEmail(), "Emails do not match");
        assertEquals(FORMATTED_FULL_NAME, applicationList.get(0).getFullName(), "Full name hasn't been formatted");
        assertEquals(application.getStatus(), applicationList.get(0).getStatus(), "Statuses do not match");
        assertNotNull(applicationList.get(0).getImage(), "Image url is null");
    }

    @Test
    void testGetApplicationsByStatus() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(get(GET_STATUS_URL, "PENDING"))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication[] arrayApplications = objectMapper.readValue(mvcResult.getResponse()
                                                                                  .getContentAsString(),
                                                                              MediaApplication[].class);

        List<MediaApplication> applicationList = Arrays.asList(arrayApplications);

        assertEquals(application.getEmail(), applicationList.get(0).getEmail(), "Emails do not match");
        assertEquals(FORMATTED_FULL_NAME, applicationList.get(0).getFullName(), "Full name hasn't been formatted");
        assertEquals(application.getStatus(), applicationList.get(0).getStatus(), "Statuses do not match");
        assertNotNull(applicationList.get(0).getImage(), "Image url is null");

    }

    @Test
    void testGetApplicationById() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(get(GET_BY_ID_URL, application.getId()))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication returnedApplication = objectMapper.readValue(mvcResult.getResponse()
                                                                                  .getContentAsString(),
                                                                              MediaApplication.class);


        assertEquals(application.getEmail(), returnedApplication.getEmail(), "Emails do not match");
        assertEquals(FORMATTED_FULL_NAME, returnedApplication.getFullName(), "Full name hasn't been formatted");
        assertEquals(application.getStatus(), returnedApplication.getStatus(), "Statuses do not match");
    }

    @Test
    void testGetApplicationByIdNotFound() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get(GET_BY_ID_URL, TEST_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().contains(String.valueOf(TEST_ID)), NOT_FOUND_ERROR);
    }

    @Test
    void testUpdateApplication() throws Exception {
        MediaApplication application = createApplication();

        assertEquals(STATUS, application.getStatus(), "Original statuses do not match");

        MvcResult mvcResult = mockMvc.perform(put(PUT_URL, application.getId(), UPDATED_STATUS))
            .andExpect(status().isOk())
            .andReturn();

        MediaApplication returnedApplication = objectMapper.readValue(mvcResult.getResponse()
                                                                                  .getContentAsString(),
                                                                              MediaApplication.class);

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
    void testDeleteApplication() throws Exception {
        MediaApplication application = createApplication();

        MvcResult mvcResult = mockMvc.perform(delete(DELETE_URL, application.getId()))
            .andExpect(status().isOk())
            .andReturn();

        assertEquals("Application deleted", mvcResult.getResponse().getContentAsString(),
                     "Application was not deleted");
    }

    @Test
    void testDeleteApplicationNotFound() throws Exception {

        MvcResult mvcResult = mockMvc.perform(delete(DELETE_URL, TEST_ID))
            .andExpect(status().isNotFound())
            .andReturn();

        assertTrue(mvcResult.getResponse().getContentAsString().contains(String.valueOf(TEST_ID)), NOT_FOUND_ERROR);
    }
}
