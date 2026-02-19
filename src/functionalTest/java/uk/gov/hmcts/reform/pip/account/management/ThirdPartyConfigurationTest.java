package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThirdPartyConfigurationTest extends AccountHelperBase {

    private static final String CONFIGURATION_PATH = "/third-party/configuration";
    private static final String USER_PATH = "/third-party";
    private static final String TEST_NAME_PREFIX = "ThirdPartyConfigurationTest";
    private static final String DESTINATION_URL = "https://example.com/callback";
    private static final String UPDATED_DESTINATION_URL = "https://example.com/callback-updated";
    private static final String TOKEN_URL = "https://example.com/token";
    private static final String CLIENT_ID_KEY = "client-id";
    private static final String CLIENT_SECRET_KEY = "client-secret";
    private static final String SCOPE_KEY = "scope";
    private static final String CREATE_SUCCESS_MSG =
        "Third-party OAuth configuration successfully created for user with ID ";
    private static final String UPDATE_SUCCESS_MSG =
        "Third-party OAuth configuration successfully updated for user with ID ";

    private String systemAdminUserId;

    @BeforeAll
    void setup() throws JsonProcessingException {
        systemAdminUserId = createSystemAdminAccount().getUserId();
    }

    @Test
    void shouldCreateOauthConfiguration() {
        UUID userId = createThirdPartyUser(TEST_NAME_PREFIX + "CreateOauthConfiguration");
        String responseMsg = createOauthConfiguration(userId);

        assertThat(responseMsg).isEqualTo(CREATE_SUCCESS_MSG + userId);
    }

    @Test
    void shouldGetConfigurationByUserId() {
        UUID userId = createThirdPartyUser(TEST_NAME_PREFIX + "GetOauthConfiguration");
        createOauthConfiguration(userId);

        Response response = doGetRequest(CONFIGURATION_PATH + "/" + userId, getAuthHeaders());
        ApiOauthConfiguration configuration = response.getBody().as(ApiOauthConfiguration.class);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(configuration.getUserId()).isEqualTo(userId);
        assertThat(configuration.getDestinationUrl()).isEqualTo(DESTINATION_URL);
        assertThat(configuration.getTokenUrl()).isEqualTo(TOKEN_URL);
        assertThat(configuration.getClientIdKey()).isEqualTo(CLIENT_ID_KEY);
        assertThat(configuration.getClientSecretKey()).isEqualTo(CLIENT_SECRET_KEY);
        assertThat(configuration.getScopeKey()).isEqualTo(SCOPE_KEY);
    }

    @Test
    void shouldUpdateConfiguration() {
        UUID userId = createThirdPartyUser(TEST_NAME_PREFIX + "UpdateOauthConfiguration");
        createOauthConfiguration(userId);

        ApiOauthConfiguration updatedConfig = new ApiOauthConfiguration();
        updatedConfig.setUserId(userId);
        updatedConfig.setDestinationUrl(UPDATED_DESTINATION_URL);
        updatedConfig.setTokenUrl(TOKEN_URL);
        updatedConfig.setScopeKey(SCOPE_KEY);
        updatedConfig.setClientIdKey(CLIENT_ID_KEY);
        updatedConfig.setClientSecretKey(CLIENT_SECRET_KEY);

        Response updateResponse = doPutRequestWithBody(CONFIGURATION_PATH + "/"
                                                           + userId, getAuthHeaders(), updatedConfig);
        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(updateResponse.getBody().asString()).isEqualTo(UPDATE_SUCCESS_MSG + userId);

        Response getResponse = doGetRequest(CONFIGURATION_PATH + "/" + userId, getAuthHeaders());
        ApiOauthConfiguration configuration = getResponse.getBody().as(ApiOauthConfiguration.class);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(configuration.getUserId()).isEqualTo(userId);
        assertThat(configuration.getDestinationUrl()).isEqualTo(UPDATED_DESTINATION_URL);
        assertThat(configuration.getTokenUrl()).isEqualTo(TOKEN_URL);
        assertThat(configuration.getClientIdKey()).isEqualTo(CLIENT_ID_KEY);
        assertThat(configuration.getClientSecretKey()).isEqualTo(CLIENT_SECRET_KEY);
        assertThat(configuration.getScopeKey()).isEqualTo(SCOPE_KEY);
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, systemAdminUserId);
        return headers;
    }

    private UUID createThirdPartyUser(String name) {
        ApiUser user = new ApiUser();
        user.setName(name);

        Response response = doPostRequest(USER_PATH, getAuthHeaders(), user);
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        return response.getBody().as(UUID.class);
    }

    private String createOauthConfiguration(UUID userId) {
        ApiOauthConfiguration config = new ApiOauthConfiguration();
        config.setUserId(userId);
        config.setDestinationUrl(DESTINATION_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setScopeKey(SCOPE_KEY);
        config.setClientIdKey(CLIENT_ID_KEY);
        config.setClientSecretKey(CLIENT_SECRET_KEY);

        Response response = doPostRequest(CONFIGURATION_PATH, getAuthHeaders(), config);
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        return response.getBody().asString();
    }
}
