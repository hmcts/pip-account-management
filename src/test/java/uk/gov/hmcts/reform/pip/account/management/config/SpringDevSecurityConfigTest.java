package uk.gov.hmcts.reform.pip.account.management.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SpringDevSecurityConfigTest {

    @Mock
    HttpSecurity httpSecurity;

    // test created for spring security when active profile is dev
    @Test
    void testSpringDevSecurityConfigCreation() throws Exception {
        SpringDevSecurityConfig springDevSecurityConfig = new SpringDevSecurityConfig();

        springDevSecurityConfig.devFilterChain(httpSecurity);

        verify(httpSecurity, times(1)).csrf(any());
        verify(httpSecurity, times(1)).authorizeHttpRequests(any());
    }
}
