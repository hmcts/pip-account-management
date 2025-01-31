package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@SuppressWarnings({"PMD.UnitTestShouldIncludeAssert"})
class ViewTest {

    @Autowired
    private transient MockMvc mockMvc;
    private static final String USERNAME = "admin";
    private static final String VALID_ROLE = "APPROLE_api.request.admin";

    //TODO: This test is currently failing due to missing subscription-management flyway scripts.
    // To be re-enabled in ticket PUB-2729
    @Disabled
    @Test
    @WithMockUser(username = USERNAME, authorities = {VALID_ROLE})
    void testRefreshView() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post("/view/refresh");
        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isOk());
    }

    @Test
    void testRefreshViewUnauthorised() throws Exception {
        MockHttpServletRequestBuilder mockHttpServletRequestBuilder = MockMvcRequestBuilders
            .post("/view/refresh");

        mockMvc.perform(mockHttpServletRequestBuilder)
            .andExpect(status().isUnauthorized());
    }
}
