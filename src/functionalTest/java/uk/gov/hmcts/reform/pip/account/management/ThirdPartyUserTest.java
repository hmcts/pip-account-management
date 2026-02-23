package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThirdPartyUserTest extends AccountHelperBase {

    private static final String BASE_PATH = "/third-party";
    private static final String BASE_NAME = "ThirdPartyUserTest";

    private String systemAdminId;

    @BeforeAll
    void setup() throws JsonProcessingException {
        PiUser systemAdminUser = createSystemAdminAccount();
        systemAdminId = systemAdminUser.getUserId();
    }

    @Test
    void shouldCreateThirdPartyUser() {
        String createdUserId = createThirdPartyUser(BASE_NAME + "Create");
        assertThat(createdUserId).isNotNull();
    }

    @Test
    void shouldGetAllThirdPartyUsers() {
        Map<String, String> headers = getAuthHeaders();
        String createdUserId = createThirdPartyUser(BASE_NAME + "GetAll");

        Response response = doGetRequest(BASE_PATH, headers);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        ApiUser[] users = response.getBody().as(ApiUser[].class);
        assertThat(users.length).isGreaterThan(0);
        assertThat(List.of(users)).anyMatch(user -> user.getUserId().toString().equals(createdUserId));
    }

    @Test
    void shouldGetThirdPartyUser() {
        Map<String, String> headers = getAuthHeaders();
        String createdUserId = createThirdPartyUser(BASE_NAME + "GetById");

        Response response = doGetRequest(BASE_PATH + "/" + createdUserId, headers);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        ApiUser retrievedUser = response.getBody().as(ApiUser.class);
        assertThat(retrievedUser.getUserId().toString()).isEqualTo(createdUserId);
        assertThat(retrievedUser.getName()).isEqualTo(BASE_NAME + "GetById");
    }

    @Test
    void shouldDeleteThirdPartyUser() {
        Map<String, String> headers = getAuthHeaders();
        String createdUserId = createThirdPartyUser(BASE_NAME + "Delete");

        Response response = doDeleteRequest(BASE_PATH + "/" + createdUserId, headers);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.getBody().asString())
            .isEqualTo("Third-party user with ID " + createdUserId + " has been deleted");
    }

    private String createThirdPartyUser(String userName) {
        ApiUser userObject = new ApiUser();
        userObject.setName(userName);

        Map<String, String> headers = getAuthHeaders();
        Response response = doPostRequest(BASE_PATH, headers, userObject);

        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        assertThat(response.getBody().asString()).isNotEmpty();

        return response.getBody().as(UUID.class).toString();
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, systemAdminId);
        return headers;
    }
}
