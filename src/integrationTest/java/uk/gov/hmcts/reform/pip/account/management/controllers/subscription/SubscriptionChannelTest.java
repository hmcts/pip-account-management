package uk.gov.hmcts.reform.pip.account.management.controllers.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationBasicTestBase;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class SubscriptionChannelTest extends IntegrationBasicTestBase {
    private static final String SUBSCRIPTION_CHANNEL_URL = "/subscription/channel";

    private ObjectMapper objectMapper;

    @Autowired
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    @DisplayName("Get all channels")
    @Test
    void testGetAllChannels() throws Exception {
        MvcResult response = mvc.perform(get(SUBSCRIPTION_CHANNEL_URL))
            .andExpect(status().isOk()).andReturn();

        Channel[] channelsReturned = objectMapper.readValue(
            response.getResponse().getContentAsString(),
            Channel[].class
        );
        assertEquals(Channel.values().length, channelsReturned.length,
                     "Correct number of channels should return"
        );
        assertArrayEquals(Channel.values(), channelsReturned, "Channels should match");
    }

    @Test
    @WithMockUser(username = "unauthorized_delete", authorities = {"APPROLE_unknown.delete"})
    void testUnauthorizedGetAllChannels() throws Exception {
        assertRequestResponseStatus(mvc, get(SUBSCRIPTION_CHANNEL_URL), FORBIDDEN.value());
    }
}
