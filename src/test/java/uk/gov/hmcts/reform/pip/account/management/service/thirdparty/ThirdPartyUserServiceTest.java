package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.ApiUserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyUserServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

    private static final String NOT_FOUND_EXCEPTION_MESSAGE = "Should throw NotFoundException if user not found";

    @Mock
    private ApiUserRepository apiUserRepository;

    @Mock
    private ThirdPartySubscriptionService thirdPartySubscriptionService;

    @Mock
    private ThirdPartyConfigurationService thirdPartyConfigurationService;

    @InjectMocks
    private ThirdPartyUserService service;

    @Test
    void testCreateThirdPartyUser() {
        ApiUser user = new ApiUser();
        when(apiUserRepository.save(user)).thenReturn(user);

        ApiUser result = service.createThirdPartyUser(user);

        assertThat(result)
            .as("Should return the saved user")
            .isSameAs(user);
    }

    @Test
    void testGetAllThirdPartyUsers() {
        when(apiUserRepository.findAll()).thenReturn(List.of(new ApiUser(), new ApiUser()));

        List<ApiUser> result = service.getAllThirdPartyUsers();

        assertThat(result)
            .as("Should return all users")
            .hasSize(2);
    }

    @Test
    void testFindThirdPartyUserWhenUserExists() {
        ApiUser user = new ApiUser();
        when(apiUserRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        ApiUser result = service.findThirdPartyUser(USER_ID);

        assertThat(result)
            .as("Should return the found user")
            .isSameAs(user);
    }

    @Test
    void testFindThirdPartyUserWhenUserDoesNotExist() {
        when(apiUserRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findThirdPartyUser(USER_ID))
            .as(NOT_FOUND_EXCEPTION_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void testDeleteThirdPartyUserWhenUserExists() {
        ApiUser user = new ApiUser();
        when(apiUserRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));

        service.deleteThirdPartyUser(USER_ID);

        verify(thirdPartySubscriptionService).deleteThirdPartySubscriptionsByUserId(USER_ID);
        verify(thirdPartyConfigurationService).deleteThirdPartyConfigurationByUserId(USER_ID);
        verify(apiUserRepository).deleteById(USER_ID);
    }

    @Test
    void testDeleteThirdPartyUserWhenUserDoesNotExist() {
        when(apiUserRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteThirdPartyUser(USER_ID))
            .as(NOT_FOUND_EXCEPTION_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void testDeleteAllThirdPartyUsersByNamePrefixSuccess() {
        UUID userId1 = UUID.randomUUID();
        UUID userId2 = UUID.randomUUID();
        UUID userId3 = UUID.randomUUID();

        ApiUser user1 = new ApiUser();
        user1.setUserId(userId1);
        ApiUser user2 = new ApiUser();
        user2.setUserId(userId2);
        ApiUser user3 = new ApiUser();
        user3.setUserId(userId3);

        String prefix = "TestUser";
        when(apiUserRepository.findAllByNameStartingWithIgnoreCase(prefix)).thenReturn(List.of(user1, user2, user3));
        when(apiUserRepository.findByUserId(userId1)).thenReturn(Optional.of(user1));
        when(apiUserRepository.findByUserId(userId2)).thenReturn(Optional.of(user2));
        when(apiUserRepository.findByUserId(userId3)).thenReturn(Optional.of(user3));

        assertThat(service.deleteAllThirdPartyUsersWithNamePrefix(prefix))
            .isEqualTo("3 third-party user(s) with name starting with " + prefix
                           + " and associated subscriptions/configurations deleted");

        verify(thirdPartySubscriptionService, times(3)).deleteThirdPartySubscriptionsByUserId(any());
        verify(thirdPartyConfigurationService, times(3)).deleteThirdPartyConfigurationByUserId(any());
        verify(apiUserRepository, times(3)).deleteById(any());
    }

    @Test
    void testDeleteAllThirdPartyUsersByNamePrefixWithNoUserMatched() {
        String prefix = "TestUser";

        when(apiUserRepository.findAllByNameStartingWithIgnoreCase(prefix)).thenReturn(List.of());

        assertThat(service.deleteAllThirdPartyUsersWithNamePrefix(prefix))
            .isEqualTo("0 third-party user(s) with name starting with " + prefix
                           + " and associated subscriptions/configurations deleted");

        verify(thirdPartySubscriptionService, never()).deleteThirdPartySubscriptionsByUserId(any());
        verify(thirdPartyConfigurationService, never()).deleteThirdPartyConfigurationByUserId(any());
        verify(apiUserRepository, never()).deleteById(any());
    }

    @Test
    void testDeleteAllThirdPartyUsersByNamePrefixWhenUserDoesNotExist() {
        String prefix = "TestUser";
        ApiUser user = new ApiUser();
        user.setUserId(USER_ID);

        when(apiUserRepository.findAllByNameStartingWithIgnoreCase(prefix)).thenReturn(List.of(user));
        when(apiUserRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteAllThirdPartyUsersWithNamePrefix(prefix))
            .as(NOT_FOUND_EXCEPTION_MESSAGE)
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }
}
