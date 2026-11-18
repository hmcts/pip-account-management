package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserCommunicationRepository;
import uk.gov.hmcts.reform.pip.account.management.model.AudienceType;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommunicationServiceTest {
    @Mock
    UserCommunicationRepository userCommunicationRepository;

    @InjectMocks
    private UserCommunicationService userCommunicationService;

    @Test
    void shouldGetAllUserCommunications() {
        UserCommunication userCommunication = new UserCommunication();
        userCommunication.setAudienceType(AudienceType.VENUE);
        userCommunication.setVenues(List.of(2, 3));
        userCommunication.setMessage("Test message");
        userCommunication.setWelshMessage("Test welsh message");

        UserCommunication userCommunication2 = new UserCommunication();
        userCommunication2.setAudienceType(AudienceType.ALL_USERS);
        userCommunication2.setMessage("Test message 2");
        userCommunication2.setWelshMessage("Test welsh message 2");

        when(userCommunicationRepository.findAll()).thenReturn(List.of(userCommunication, userCommunication2));

        assertThat(userCommunicationService.getAll())
            .hasSize(2)
            .containsExactly(userCommunication, userCommunication2);
    }
}
