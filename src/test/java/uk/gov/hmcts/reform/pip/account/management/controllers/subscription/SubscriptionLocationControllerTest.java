package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionLocationService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionLocationControllerTest {

    private List<Subscription> mockSubscriptionList;

    private static final String STATUS_CODE_MATCH = "Status codes should match";
    private static final String LOCATION_ID = "1";
    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    SubscriptionLocationService subscriptionLocationService;

    @InjectMocks
    SubscriptionLocationController subscriptionLocationController;

    @Test
    void testFindSubscriptionsByLocationId() {
        when(subscriptionLocationService.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        assertEquals(mockSubscriptionList,
                     subscriptionLocationController.findSubscriptionsByLocationId(LOCATION_ID).getBody(),
                     "The found subscription does not match expected subscription");
    }

    @Test
    void testFindSubscriptionsByLocationIdReturnsOk() {
        when(subscriptionLocationService.findSubscriptionsByLocationId(LOCATION_ID))
            .thenReturn(mockSubscriptionList);
        assertEquals(HttpStatus.OK, subscriptionLocationController.findSubscriptionsByLocationId(LOCATION_ID)
            .getStatusCode(), STATUS_CODE_MATCH);
    }

    @Test
    void testDeleteSubscriptionByLocationReturnsOk() {
        when(subscriptionLocationService.deleteSubscriptionByLocation(LOCATION_ID, USER_ID))
            .thenReturn("Total 10 subscriptions deleted for location id");

        assertEquals(HttpStatus.OK, subscriptionLocationController.deleteSubscriptionByLocation(
                         USER_ID, Integer.parseInt(LOCATION_ID)).getStatusCode(),
                     "Delete subscription location endpoint has not returned OK");
    }
}
