package uk.gov.hmcts.reform.pip.account.management.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.AudienceType;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;
import uk.gov.hmcts.reform.pip.account.management.service.UserCommunicationService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommunicationControllerTest {
    private static final UUID ID = UUID.randomUUID();
    private static final UUID REQUESTER_ID = UUID.randomUUID();

    private static UserCommunication userCommunication;
    private static UserCommunication userCommunication2;

    @Mock
    UserCommunicationService userCommunicationService;

    @InjectMocks
    UserCommunicationController userCommunicationController;

    @BeforeAll
    public static void setup() {
        userCommunication = new UserCommunication();
        userCommunication.setAudienceType(AudienceType.VENUE);
        userCommunication.setVenues(List.of(2, 3));
        userCommunication.setMessage("Test message");
        userCommunication.setWelshMessage("Test welsh message");

        userCommunication2 = new UserCommunication();
        userCommunication2.setAudienceType(AudienceType.ALL_USERS);
        userCommunication2.setMessage("Test message 2");
        userCommunication2.setWelshMessage("Test welsh message 2");
    }

    @Test
    void testGetUserCommunications() {
        when(userCommunicationService.getAll()).thenReturn(List.of(userCommunication, userCommunication2));

        ResponseEntity<List<UserCommunication>> response = userCommunicationController.getUserCommunications();

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .isEqualTo(List.of(userCommunication, userCommunication2));
    }

    @Test
    void testFindUserCommunicationById() {
        when(userCommunicationService.findUserCommunicationById(ID)).thenReturn(userCommunication);

        ResponseEntity<UserCommunication> response = userCommunicationController
            .getUserCommunicationById(ID, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .isEqualTo(userCommunication);
    }

    @Test
    void testCreateUserCommunication() {
        when(userCommunicationService.createUserCommunication(userCommunication)).thenReturn(userCommunication);

        ResponseEntity<String> response = userCommunicationController
            .createUserCommunication(userCommunication, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
            .isEqualTo("User communication successfully created for VENUE audience type");
    }

    @Test
    void testUpdateUserCommunication() {
        when(userCommunicationService.updateUserCommunication(userCommunication, ID)).thenReturn(userCommunication);

        ResponseEntity<String> response = userCommunicationController
            .updateUserCommunication(ID, userCommunication, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .isEqualTo("User communication with ID " + ID + " successfully updated");
    }

    @Test
    void testDeleteUserCommunication() {
        assertThat(userCommunicationController.deleteUserCommunication(ID, REQUESTER_ID).getStatusCode())
            .isEqualTo(HttpStatus.NO_CONTENT);
    }
}
