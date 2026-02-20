package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ThirdPartySubscriptionTest extends AccountHelperBase {

    private static final String BASE_PATH = "/third-party/subscription";
    private static final String API_USER_PATH = "/third-party";
    private static final String BASE_NAME = "ThirdPartySubscriptionTest";
    private static final String SUCCESS_CREATE_MSG = "Third-party subscriptions successfully created for user with ID ";
    private static final String SUCCESS_UPDATE_MSG = "Third-party subscriptions successfully updated for user with ID ";

    private String systemAdminId;

    @BeforeAll
    void setup() throws JsonProcessingException {
        systemAdminId = createSystemAdminAccount().getUserId();
    }

    @Test
    void shouldCreateThirdPartySubscription() {
        Map<String, String> headerMap = getAuthHeaders();
        UUID userId = createThirdPartyUser(BASE_NAME + "CreateApiSubscription");
        String responseMsg = createThirdPartyApiSubscription(userId, headerMap);

        assertThat(responseMsg).isEqualTo(SUCCESS_CREATE_MSG + userId);
    }

    @Test
    void shouldGetThirdPartySubscriptionByUserId() {
        Map<String, String> headerMap = getAuthHeaders();
        UUID userId = createThirdPartyUser(BASE_NAME + "GetApiSubscription");
        createThirdPartyApiSubscription(userId, headerMap);

        Response response = doGetRequest(BASE_PATH + "/" + userId, headerMap);
        ApiSubscription[] apiSubscriptions = response.getBody().as(ApiSubscription[].class);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(apiSubscriptions).isNotEmpty();
        assertThat(List.of(apiSubscriptions))
            .anyMatch(sub -> sub.getUserId().equals(userId));
    }

    @Test
    void shouldUpdateThirdPartySubscription() {
        Map<String, String> headerMap = getAuthHeaders();
        UUID userId = createThirdPartyUser(BASE_NAME + "UpdateApiSubscription");
        createThirdPartyApiSubscription(userId, headerMap);

        ApiSubscription updatedSubscription = new ApiSubscription();
        updatedSubscription.setUserId(userId);
        updatedSubscription.setSensitivity(Sensitivity.PRIVATE);
        updatedSubscription.setListType(ListType.CIVIL_DAILY_CAUSE_LIST);

        Response response = doPutRequestWithBody(BASE_PATH + "/" + userId, headerMap, List.of(updatedSubscription));
        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        assertThat(response.getBody().asString()).isEqualTo(SUCCESS_UPDATE_MSG + userId);

        Response getResponse = doGetRequest(BASE_PATH + "/" + userId, headerMap);
        ApiSubscription[] apiSubscriptions = getResponse.getBody().as(ApiSubscription[].class);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(List.of(apiSubscriptions))
            .anyMatch(sub -> sub.getListType().equals(ListType.CIVIL_DAILY_CAUSE_LIST)
                && sub.getSensitivity().equals(Sensitivity.PRIVATE));
    }

    private Map<String, String> getAuthHeaders() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, systemAdminId);
        return headers;
    }

    private UUID createThirdPartyUser(String userName) {
        ApiUser user = new ApiUser();
        user.setName(userName);

        Response response = doPostRequest(API_USER_PATH, getAuthHeaders(), user);
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        return response.getBody().as(UUID.class);
    }

    private String createThirdPartyApiSubscription(UUID userId, Map<String, String> headerMap) {
        ApiSubscription subscription = new ApiSubscription();
        subscription.setUserId(userId);
        subscription.setSensitivity(Sensitivity.PUBLIC);
        subscription.setListType(ListType.CARE_STANDARDS_LIST);

        Response response = doPostRequest(BASE_PATH, headerMap, List.of(subscription));
        assertThat(response.getStatusCode()).isEqualTo(CREATED.value());
        return response.getBody().asString();
    }
}
