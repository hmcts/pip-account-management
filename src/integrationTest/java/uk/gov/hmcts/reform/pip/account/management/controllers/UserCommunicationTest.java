package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.pip.account.management.model.AudienceType;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;
import uk.gov.hmcts.reform.pip.account.management.service.UserCommunicationService;
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.UserCommunicationAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserCommunicationTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String USER_COMMUNICATION_URL = "/communication";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final UUID REQUESTER_ID = UUID.randomUUID();

    private UserCommunication userCommunication;
    private UserCommunication userCommunication2;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCommunicationService userCommunicationService;

    @MockitoBean
    private UserCommunicationAuthorisationService userCommunicationAuthorisationService;

    @BeforeAll
    public void setup() {
        userCommunication = new UserCommunication();
        userCommunication.setAudienceType(AudienceType.VENUE);
        userCommunication.setVenues(List.of(2, 3));
        userCommunication.setMessage("Test message");
        userCommunication.setWelshMessage("Test Welsh message");

        userCommunication2 = new UserCommunication();
        userCommunication2.setAudienceType(AudienceType.ALL_USERS);
        userCommunication2.setMessage("Test message 2");
        userCommunication2.setWelshMessage("Test welsh message 2");
    }

    @BeforeEach
    void setupEach() {
        when(userCommunicationAuthorisationService.userCanManageUserCommunication(REQUESTER_ID)).thenReturn(true);
    }

    private MvcResult createUserCommunicationRequest() throws Exception {

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .post(USER_COMMUNICATION_URL)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .content(OBJECT_MAPPER.writeValueAsString(userCommunication))
            .contentType(MediaType.APPLICATION_JSON);

        return mockMvc.perform(requestBuilder)
            .andExpect(status().isCreated())
            .andReturn();
    }

    private MvcResult retrieveAllUserCommunicationsRequest() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .get(USER_COMMUNICATION_URL)
            .contentType(MediaType.APPLICATION_JSON);

        return mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void testCreateUserCommunicationSuccess() throws Exception {
        MvcResult postResponse = createUserCommunicationRequest();

        assertThat(postResponse.getResponse().getContentAsString())
            .isEqualTo("User communication successfully created for VENUE audience type");

        MvcResult results = retrieveAllUserCommunicationsRequest();

        List<UserCommunication> returnedUserCommunication = OBJECT_MAPPER.readValue(
            results.getResponse().getContentAsString(),
            OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, UserCommunication.class)
        );

        UserCommunication userCommunication = returnedUserCommunication.get(0);
        assertThat(userCommunication.getAudienceType()).isEqualTo(AudienceType.VENUE);
        assertThat(userCommunication.getVenues()).isEqualTo(List.of(2, 3));
        assertThat(userCommunication.getMessage()).isEqualTo("Test message");
        assertThat(userCommunication.getWelshMessage()).isEqualTo("Test welsh message");

    }

    @Test
    void testCreateUserCommunication() {

    }
}
