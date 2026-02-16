package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThirdPartySubscriptionTest extends AccountHelperBase {

    private static final String BASE_PATH = "/third-party";

    private String systemAdminId;

    @BeforeAll
    void setup() throws JsonProcessingException {
        PiUser systemAdminUser = createSystemAdminAccount();
        systemAdminId = systemAdminUser.getUserId();
    }

    @Test
    void shouldCreateThirdPartyUser() {

        ApiUser createdUser = createThirdPartyUser("createThirdPartyUserTest");

        assertThat(createdUser).isNotNull();
        assertThat(createdUser.getUserId()).isNotNull();
        assertThat(createdUser.getName()).isEqualTo("createThirdPartyUserTest");
    }

    private ApiUser createThirdPartyUser(String userName) {

        ApiUser userObject = new ApiUser();
        userObject.setName(userName);

        Map<String, String> headerMap = new ConcurrentHashMap<>(bearer);
        headerMap.put(REQUESTER_ID_HEADER, systemAdminId);

        Response response = doPostRequest(
            BASE_PATH,
            headerMap,
            userObject
        );

        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        assertThat(response.getBody().asString()).isNotEmpty();

        return response.getBody().as(ApiUser.class);
    }

    @Test
    void shouldGetAllThirdPartyUsers() {

        Map<String, String> headerMap = new ConcurrentHashMap<>(bearer);
        headerMap.put(REQUESTER_ID_HEADER, systemAdminId);

        ApiUser createdUser = createThirdPartyUser("testGetAllUsers");

        Response response = doGetRequest(
            BASE_PATH,
            headerMap
        );

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        ApiUser[] users = response.getBody().as(ApiUser[].class);

        assertThat(users.length).isGreaterThan(0);

        // Safer assertion: do not assume ordering
        assertThat(Arrays.stream(users)
                       .anyMatch(user -> user.getUserId().equals(createdUser.getUserId())))
            .isTrue();
    }

    @Test
    void shouldGetThirdPartyUser() {

        Map<String, String> headerMap = new ConcurrentHashMap<>(bearer);
        headerMap.put(REQUESTER_ID_HEADER, systemAdminId);

        ApiUser createdUser = createThirdPartyUser("testGetUserById");

        Response response = doGetRequest(
            BASE_PATH + "/" + createdUser.getUserId(),
            headerMap
        );

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        ApiUser retrievedUser = response.getBody().as(ApiUser.class);

        assertThat(retrievedUser.getUserId())
            .isEqualTo(createdUser.getUserId());

        assertThat(retrievedUser.getName())
            .isEqualTo("testGetUserById");
    }

    @Test
    void shouldDeleteThirdPartyUser() {

        Map<String, String> headerMap = new ConcurrentHashMap<>(bearer);
        headerMap.put(REQUESTER_ID_HEADER, systemAdminId);

        ApiUser createdUser = createThirdPartyUser("testDeleteUserById");

        Response response = doDeleteRequest(
            BASE_PATH + "/" + createdUser.getUserId(),
            headerMap
        );

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.getBody().asString()).isEmpty();
    }
}
