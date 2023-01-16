package uk.gov.hmcts.reform.pip.account.management.controllers;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.Application;
import uk.gov.hmcts.reform.pip.account.management.config.AzureConfigurationClientTestConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = {AzureConfigurationClientTestConfiguration.class, Application.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles(profiles = "functional")
@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
@SuppressWarnings({"PMD.TooManyMethods", "PMD.JUnitTestsShouldIncludeAssert"})
class InactiveAccountManagementTest {
    private static final String ROOT_URL = "/account";
    private static final String NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive/notify";
    private static final String DELETE_EXPIRED_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive";
    private static final String NOTIFY_INACTIVE_ADMIN_ACCOUNTS_URL = ROOT_URL + "/admin/inactive/notify";
    private static final String DELETE_EXPIRED_ADMIN_ACCOUNTS_URL = ROOT_URL + "/admin/inactive";
    private static final String NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive/notify";
    private static final String DELETE_EXPIRED_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive";

    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";
    private static final String FORBIDDEN_STATUS_CODE = "Status code does not match forbidden";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testNotifyInactiveMediaAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedNotifyInactiveMediaAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testDeleteExpiredMediaAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_MEDIA_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDeleteExpiredMediaAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_MEDIA_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testNotifyInactiveAdminAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_ADMIN_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedNotifyInactiveAdminAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_ADMIN_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testDeleteExpiredAdminAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_ADMIN_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDeleteExpiredAdminAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_ADMIN_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testNotifyInactiveIdamAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedNotifyInactiveIdamAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .post(NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }

    @Test
    void testDeleteExpiredIdamAccountsSuccess() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_IDAM_ACCOUNTS_URL);

        mockMvc.perform(request).andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorizedDeleteExpiredIdamAccounts() throws Exception {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders
            .delete(DELETE_EXPIRED_IDAM_ACCOUNTS_URL);

        MvcResult mvcResult = mockMvc.perform(request).andExpect(status().isForbidden()).andReturn();

        assertEquals(FORBIDDEN.value(), mvcResult.getResponse().getStatus(),
                     FORBIDDEN_STATUS_CODE
        );
    }
}
