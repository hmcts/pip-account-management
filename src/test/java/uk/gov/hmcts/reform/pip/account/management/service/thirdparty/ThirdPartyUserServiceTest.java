package uk.gov.hmcts.reform.pip.account.management.service.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.pip.account.management.database.ApiUserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUserStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyUserServiceTest {
    private static final UUID USER_ID = UUID.randomUUID();

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
            .as("Should throw NotFoundException if user not found")
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }

    @Test
    void testUpdateThirdPartyUserStatusWhenUserExists() {
        ApiUser user = new ApiUser();
        user.setUserId(USER_ID);
        user.setStatus(ApiUserStatus.Pending);
        final ApiUserStatus newStatus = ApiUserStatus.Active;

        when(apiUserRepository.findByUserId(USER_ID)).thenReturn(Optional.of(user));
        when(apiUserRepository.save(user)).thenReturn(user);

        ApiUser result = service.updateThirdPartyUserStatus(USER_ID, newStatus);

        assertThat(result.getStatus())
            .as("Should update the user's status")
            .isEqualTo(newStatus);
        verify(apiUserRepository).save(user);
    }

    @Test
    void testUpdateThirdPartyUserStatusWhenUserDoesNotExist() {
        ApiUserStatus newStatus = ApiUserStatus.Active;
        when(apiUserRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateThirdPartyUserStatus(USER_ID, newStatus))
            .as("Should throw NotFoundException if user not found")
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
            .as("Should throw NotFoundException if user not found")
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining(USER_ID.toString());
    }
}
