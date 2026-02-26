package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUserStatus;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartyUserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartyUserControllerTest {
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String OK_MESSAGE = "Response status should be OK";

    @Mock
    private ThirdPartyUserService thirdPartyUserService;

    @InjectMocks
    private ThirdPartyUserController controller;

    @Test
    void testCreateThirdPartyUser() {
        ApiUser inputUser = new ApiUser();
        ApiUser createdUser = new ApiUser();
        createdUser.setUserId(USER_ID);

        when(thirdPartyUserService.createThirdPartyUser(inputUser)).thenReturn(createdUser);

        ResponseEntity<UUID> response = controller.createThirdPartyUser(inputUser, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be CREATED")
            .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
            .as("Response body should contain the user ID")
            .isEqualTo(USER_ID);

        verify(thirdPartyUserService).createThirdPartyUser(inputUser);
    }

    @Test
    void testGetAllThirdPartyUsers() {
        when(thirdPartyUserService.getAllThirdPartyUsers())
            .thenReturn(List.of(new ApiUser(), new ApiUser(), new ApiUser()));

        ResponseEntity<List<ApiUser>> response = controller.getAllThirdPartyUsers(REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as(OK_MESSAGE)
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should contain the list of ApiUsers")
            .hasSize(3);
    }

    @Test
    void testGetThirdPartyUserByUserId() {
        ApiUser apiUser = new ApiUser();
        apiUser.setUserId(USER_ID);

        when(thirdPartyUserService.findThirdPartyUser(USER_ID)).thenReturn(apiUser);

        ResponseEntity<ApiUser> response = controller.getThirdPartyUserByUserId(USER_ID, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as(OK_MESSAGE)
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should be the expected ApiUser")
            .isEqualTo(apiUser);
    }

    @Test
    void testUpdateThirdPartyUserStatus() {
        ApiUserStatus newStatus = ApiUserStatus.ACTIVE;
        ApiUser updatedUser = new ApiUser();
        updatedUser.setUserId(USER_ID);
        updatedUser.setStatus(newStatus);

        when(thirdPartyUserService.updateThirdPartyUserStatus(USER_ID, newStatus)).thenReturn(updatedUser);

        ResponseEntity<ApiUser> response = controller.updateUserStatus(USER_ID, newStatus, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be OK")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should contain the updated user")
            .isEqualTo(updatedUser);

        verify(thirdPartyUserService).updateThirdPartyUserStatus(USER_ID, newStatus);
    }


    @Test
    void testDeleteThirdPartyUser() {
        ResponseEntity<String> response = controller.deleteThirdPartyUser(USER_ID, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as(OK_MESSAGE)
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should contain the user ID")
            .contains(USER_ID.toString());

        verify(thirdPartyUserService).deleteThirdPartyUser(USER_ID);
    }
}
