package uk.gov.hmcts.reform.pip.account.management.controllers.thirdparty;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.service.thirdparty.ThirdPartySubscriptionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ThirdPartySubscriptionControllerTest {
    private static final UUID REQUESTER_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    private ThirdPartySubscriptionService thirdPartySubscriptionService;

    @InjectMocks
    private ThirdPartySubscriptionController controller;

    @Test
    void testCreateThirdPartySubscriptions() {
        ApiSubscription sub = new ApiSubscription();
        sub.setUserId(USER_ID);
        List<ApiSubscription> subs = Collections.singletonList(sub);

        ResponseEntity<String> response = controller.createThirdPartySubscriptions(subs, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be CREATED")
            .isEqualTo(HttpStatus.CREATED);

        assertThat(response.getBody())
            .as("Response body should contain the user ID")
            .contains(USER_ID.toString());

        verify(thirdPartySubscriptionService).createThirdPartySubscriptions(subs);
    }

    @Test
    void testCreateThirdPartySubscriptions_emptyList() {
        List<ApiSubscription> subs = new ArrayList<>();

        ResponseEntity<String> response = controller.createThirdPartySubscriptions(subs, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be BAD_REQUEST for empty list")
            .isEqualTo(HttpStatus.BAD_REQUEST);

        assertThat(response.getBody())
            .as("Response body should contain error message for empty list")
            .contains("cannot be empty");

        verify(thirdPartySubscriptionService, never()).createThirdPartySubscriptions(any());
    }

    @Test
    void testGetThirdPartySubscriptions() {
        List<ApiSubscription> subs = Collections.singletonList(new ApiSubscription());

        when(thirdPartySubscriptionService.findThirdPartySubscriptionsByUserId(USER_ID)).thenReturn(subs);

        ResponseEntity<List<ApiSubscription>> response = controller.getThirdPartySubscriptions(USER_ID, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be OK")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should be the expected subscriptions list")
            .isEqualTo(subs);

        verify(thirdPartySubscriptionService).findThirdPartySubscriptionsByUserId(USER_ID);
    }

    @Test
    void testUpdateThirdPartySubscriptions() {
        List<ApiSubscription> subs = Collections.singletonList(new ApiSubscription());

        ResponseEntity<String> response = controller.updateThirdPartySubscriptions(USER_ID, subs, REQUESTER_ID);

        assertThat(response.getStatusCode())
            .as("Response status should be OK")
            .isEqualTo(HttpStatus.OK);

        assertThat(response.getBody())
            .as("Response body should contain the user ID")
            .contains(USER_ID.toString());

        verify(thirdPartySubscriptionService).updateThirdPartySubscriptionsByUserId(USER_ID, subs);
    }
}
