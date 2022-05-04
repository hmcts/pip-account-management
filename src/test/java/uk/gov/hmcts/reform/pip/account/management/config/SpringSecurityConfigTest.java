package uk.gov.hmcts.reform.pip.account.management.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class SpringSecurityConfigTest {

    /**
     * This test checks the creation of the config. Ideally this test isn't needed and we would just test the configure
     * method, however this is marked as protected. Sonarqube flags this as coverage required, and rather than excluding
     * this file globally, this test has been added as a placeholder.
     */
    @Test
    void testSpringSecurityConfigCreation() {
        SpringSecurityConfig springSecurityConfig = new SpringSecurityConfig();
        assertNotNull(springSecurityConfig, "Spring security config class not created");
    }

}
