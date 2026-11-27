package uk.gov.hmcts.reform.pip.account.management.controllers;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommunicationControllerTest {
    @Mock
    UserCommunicationService userCommunicationService;

    @InjectMocks
    UserCommunicationController userCommunicationController;

    @Test
    void testGetUserCommunications() {
        UserCommunication userCommunication = new UserCommunication();
        userCommunication.setAudienceType(AudienceType.VENUE);
        userCommunication.setVenues(List.of(2, 3));
        userCommunication.setMessage("Test message");
        userCommunication.setWelshMessage("Test welsh message");

        UserCommunication userCommunication2 = new UserCommunication();
        userCommunication2.setAudienceType(AudienceType.ALL_USERS);
        userCommunication2.setMessage("Test message 2");
        userCommunication2.setWelshMessage("Test welsh message 2");

        when(userCommunicationService.getAll()).thenReturn(List.of(userCommunication, userCommunication2));

        ResponseEntity<List<UserCommunication>> response = userCommunicationController.getUserCommunications();

        assertThat(response.getStatusCode())
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .isEqualTo(List.of(userCommunication, userCommunication2));
    }
}
