package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.pip.account.management.service.subscription.SubscriptionChannelService;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionChannelControllerTest {

    @Mock
    SubscriptionChannelService subscriptionChannelService;

    @InjectMocks
    SubscriptionChannelController subscriptionChannelController;

    @Test
    void retrieveChannels() {
        List<Channel> channelList = List.of(Channel.EMAIL);
        when(subscriptionChannelService.retrieveChannels()).thenReturn(channelList);

        ResponseEntity<List<Channel>> channels = subscriptionChannelController.retrieveChannels(UUID.randomUUID(),
                                                                                                "456");

        assertEquals(channelList, channels.getBody(), "Unexpected list of channels returned");
        assertEquals(HttpStatus.OK, channels.getStatusCode(), "Unexpected status returned");
    }
}
