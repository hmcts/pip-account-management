package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@TestPropertySource(properties = "migrationSupport.enableApi=false")
class MigrationSupportDisabledApiTest extends IntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testEndpointNotAvailableWhenMigrationSupportDisabled() throws Exception {
        mockMvc.perform(get("/migration-support/account"))
            .andExpect(status().isNotFound());
    }
}
