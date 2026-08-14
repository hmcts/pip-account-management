package uk.gov.hmcts.reform.pip.account.management.utils;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import uk.gov.hmcts.reform.pip.account.management.service.PublicationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
public class IntegrationTestBase {
    private static final String BLOB_IMAGE_URL = "https://localhost";

    @MockitoBean
    protected PublicationService publicationService;

    @MockitoBean
    protected BlobContainerClient blobContainerClient;

    @MockitoBean
    protected BlobClient blobClient;

    @BeforeEach
    void setupBlobClient() {
        when(blobContainerClient.getBlobClient(any())).thenReturn(blobClient);
        when(blobContainerClient.getBlobContainerUrl()).thenReturn(BLOB_IMAGE_URL);
    }

    protected void assertRequestResponseStatus(MockMvc mockMvc, MockHttpServletRequestBuilder request,
                                               int statusCode) throws Exception {
        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().is(statusCode)).andReturn();

        assertEquals(statusCode, mvcResult.getResponse().getStatus(),
                     String.format("Response does not match expected status code %s", statusCode)
        );
    }
}
