package uk.gov.hmcts.reform.pip.account.management.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class WebClientConfigurationTest {

    @Test
    void testWebClientIsInitialised() {
        WebClientConfig webClientConfig = new WebClientConfig();

        assertTrue(
            webClientConfig.toString().contains("uk.gov.hmcts.reform.pip.account.management.config"
                                                    + ".WebClientConfig"),
            "hello"
        );

    }
}
