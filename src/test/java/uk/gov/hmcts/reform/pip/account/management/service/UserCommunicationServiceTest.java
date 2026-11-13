package uk.gov.hmcts.reform.pip.account.management.service;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.UserCommunicationRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.AudienceType;
import uk.gov.hmcts.reform.pip.account.management.model.UserCommunication;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCommunicationServiceTest {
    private static final UUID ID = UUID.randomUUID();
    private static UserCommunication userCommunication;
    private static UserCommunication userCommunication2;

    @Mock
    UserCommunicationRepository userCommunicationRepository;

    @InjectMocks
    private UserCommunicationService userCommunicationService;

    @BeforeAll
    static void setUp() {
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
    void shouldGetAllUserCommunications() {
        when(userCommunicationRepository.findAll()).thenReturn(List.of(userCommunication, userCommunication2));

        assertThat(userCommunicationService.getAll())
            .hasSize(2)
            .containsExactly(userCommunication, userCommunication2);
    }

    @Test
    void shouldFindUserCommunicationByIdWhenExists() {
        when(userCommunicationRepository.findById(ID)).thenReturn(Optional.of(userCommunication));

        assertThat(userCommunicationService.findUserCommunicationById(ID))
            .isEqualTo(userCommunication);
    }

    @Test
    void testFindUserCommunicationByIdNotFound() {
        when(userCommunicationRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCommunicationService.findUserCommunicationById(ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User communication with ID %s could not be found", ID));
    }

    @Test
    void shouldCreateUserCommunication() {
        when(userCommunicationRepository.save(userCommunication)).thenReturn(userCommunication);

        assertThat(userCommunicationService.createUserCommunication(userCommunication))
            .isEqualTo(userCommunication);
    }

    @Test
    void shouldUpdateUserCommunication() {
        when(userCommunicationRepository.findById(ID)).thenReturn(Optional.of(userCommunication));
        when(userCommunicationRepository.save(userCommunication2)).thenReturn(userCommunication2);

        assertThat(userCommunicationService.updateUserCommunication(userCommunication2, ID))
            .isEqualTo(userCommunication2);
    }

    @Test
    void testUpdateUserCommunicationNotFound() {
        when(userCommunicationRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCommunicationService.updateUserCommunication(userCommunication, ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User communication with ID %s could not be found", ID));

        verify(userCommunicationRepository, never()).save(userCommunication);
    }

    @Test
    void testDeleteUserCommunicationNotFound() {
        when(userCommunicationRepository.findById(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userCommunicationService.deleteUserCommunication(ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessage(String.format("User communication with ID %s could not be found", ID));

        verify(userCommunicationRepository, never()).deleteById(ID);
    }
}
