package uk.gov.hmcts.reform.pip.account.management.controllers.account;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class InactiveAccountManagementTest extends IntegrationTestBase {
    private static final String ROOT_URL = "/account";
    private static final String NOTIFY_INACTIVE_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive/notify";
    private static final String DELETE_EXPIRED_MEDIA_ACCOUNTS_URL = ROOT_URL + "/media/inactive";
    private static final String DELETE_EXPIRED_ADMIN_ACCOUNTS_URL = ROOT_URL + "/admin/inactive";
    private static final String NOTIFY_INACTIVE_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive/notify";
    private static final String DELETE_EXPIRED_IDAM_ACCOUNTS_URL = ROOT_URL + "/idam/inactive";

    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

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

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
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

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
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

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
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

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
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

        assertRequestResponseStatus(mockMvc, request, FORBIDDEN.value());
    }
}
