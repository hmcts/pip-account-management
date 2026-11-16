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
import uk.gov.hmcts.reform.pip.account.management.service.authorisation.UserCommunicationAuthorisationService;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserCommunicationTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String USER_COMMUNICATION_URL = "/communication";
    private static final String REQUESTER_ID_HEADER = "x-requester-id";
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    private UserCommunication userCommunication;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCommunicationAuthorisationService userCommunicationAuthorisationService;

    @BeforeAll
    public void setup() {
        userCommunication = new UserCommunication();
        userCommunication.setAudienceType(AudienceType.VENUE);
        userCommunication.setVenues(List.of(2, 3));
        userCommunication.setMessage("Test message");
        userCommunication.setWelshMessage("Test Welsh message");
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

    private MvcResult retrieveUserCommunicationByIdRequest(String userCommunicationId) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .get(USER_COMMUNICATION_URL + "/" + userCommunicationId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        return mockMvc.perform(requestBuilder)
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    void testCreateUserCommunicationSuccess() throws Exception {
        MvcResult postResponse = createUserCommunicationRequest();

        String userCommunicationId = postResponse.getResponse().getContentAsString();
        assertThat(userCommunicationId)
            .isNotBlank();

        MvcResult result = retrieveUserCommunicationByIdRequest(userCommunicationId);

        UserCommunication returnedUserCommunication = OBJECT_MAPPER.readValue(
            result.getResponse().getContentAsString(),
            UserCommunication.class
        );

        assertThat(returnedUserCommunication.getAudienceType()).isEqualTo(AudienceType.VENUE);
        assertThat(returnedUserCommunication.getVenues()).isEqualTo(List.of(2, 3));
        assertThat(returnedUserCommunication.getMessage()).isEqualTo("Test message");
        assertThat(returnedUserCommunication.getWelshMessage()).isEqualTo("Test Welsh message");
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testCreateUserCommunicationForbidden() throws Exception {
        when(userCommunicationAuthorisationService.userCanManageUserCommunication(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .post(USER_COMMUNICATION_URL)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .content(OBJECT_MAPPER.writeValueAsString(userCommunication))
            .contentType(MediaType.APPLICATION_JSON);

        assertRequestResponseStatus(mockMvc, requestBuilder, FORBIDDEN.value());
    }

    @Test
    void testUpdateUserCommunicationSuccess() throws Exception {
        MvcResult postResponse = createUserCommunicationRequest();

        String userCommunicationId = postResponse.getResponse().getContentAsString();
        assertThat(userCommunicationId)
            .isNotBlank();

        UserCommunication userCommunicationToUpdate = new UserCommunication();
        userCommunicationToUpdate.setId(UUID.fromString(userCommunicationId));
        userCommunicationToUpdate.setAudienceType(AudienceType.VENUE);
        userCommunicationToUpdate.setVenues(List.of(2));
        userCommunicationToUpdate.setMessage("Updated test message");

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .put(USER_COMMUNICATION_URL + "/" + userCommunicationId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .content(OBJECT_MAPPER.writeValueAsString(userCommunicationToUpdate))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder).andExpect(status().isOk());

        MvcResult result = retrieveUserCommunicationByIdRequest(userCommunicationId);

        UserCommunication returnedUserCommunication = OBJECT_MAPPER.readValue(
            result.getResponse().getContentAsString(),
            UserCommunication.class
        );

        assertThat(returnedUserCommunication.getAudienceType()).isEqualTo(AudienceType.VENUE);
        assertThat(returnedUserCommunication.getVenues()).isEqualTo(List.of(2));
        assertThat(returnedUserCommunication.getMessage()).isEqualTo("Updated test message");
    }

    @Test
    void testUpdateUserCommunicationNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .put(USER_COMMUNICATION_URL + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .content(OBJECT_MAPPER.writeValueAsString(userCommunication))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUpdateUserCommunicationForbidden() throws Exception {
        when(userCommunicationAuthorisationService.userCanManageUserCommunication(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .put(USER_COMMUNICATION_URL + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .content(OBJECT_MAPPER.writeValueAsString(userCommunication))
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
    }

    @Test
    void testDeleteUserCommunicationSuccess() throws Exception {
        MvcResult postResponse = createUserCommunicationRequest();

        String userCommunicationId = postResponse.getResponse().getContentAsString();
        assertThat(userCommunicationId)
            .isNotBlank();

        MockHttpServletRequestBuilder postRequestBuilder = MockMvcRequestBuilders
            .delete(USER_COMMUNICATION_URL + "/" + userCommunicationId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mockMvc.perform(postRequestBuilder)
            .andExpect(status().isOk());

        MockHttpServletRequestBuilder getRequestBuilder = MockMvcRequestBuilders
            .get(USER_COMMUNICATION_URL + "/" + userCommunicationId)
            .header(REQUESTER_ID_HEADER, REQUESTER_ID)
            .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequestBuilder)
            .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteUserCommunicationNotFound() throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .delete(USER_COMMUNICATION_URL + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mockMvc.perform(requestBuilder).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testDeleteUserCommunicationForbidden() throws Exception {
        when(userCommunicationAuthorisationService.userCanManageUserCommunication(REQUESTER_ID)).thenReturn(false);

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
            .delete(USER_COMMUNICATION_URL + "/" + UUID.randomUUID())
            .header(REQUESTER_ID_HEADER, REQUESTER_ID);

        mockMvc.perform(requestBuilder).andExpect(status().isForbidden());
    }
}
