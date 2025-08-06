package uk.gov.hmcts.reform.pip.account.management.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FunctionalTestBase {
    protected static final String CONTENT_TYPE_VALUE = "application/json";

    @Autowired
    private OAuthClient authClient;

    protected String dataManagementAccessToken;
    protected static final String BEARER = "Bearer ";
    protected Map<String, String> bearer;
    protected ObjectMapper objectMapper = new ObjectMapper();

    //Testing Support URLs
    protected static final String TESTING_SUPPORT_DELETE_ACCOUNT_URL = "/testing-support/account/";
    protected static final String TESTING_SUPPORT_SUBSCRIPTION_URL = "/testing-support/subscription/";
    protected static final String TESTING_SUPPORT_LOCATION_URL = "/testing-support/location/";
    protected static final String TESTING_SUPPORT_AUDIT_URL = "/testing-support/audit/";
    protected static final String TESTING_SUPPORT_APPLICATION_URL = "/testing-support/application/";

    //Account URLs
    protected static final String ACCOUNT_URL = "/account";
    protected static final String GET_BY_USER_ID = "/account/%s";
    protected static final String CREATE_PI_ACCOUNT = "/account/add/pi";
    protected static final String CREATE_SYSTEM_ADMIN_SSO = "/account/system-admin";
    protected static final String ADD_SYSTEM_ADMIN_B2C_URL = ACCOUNT_URL + "/add/system-admin";
    protected static final String GET_ALL_THIRD_PARTY_ACCOUNTS = "/account/all/third-party";
    protected static final String GET_ADMIN_USER_BY_EMAIL_AND_PROVENANCE = "/account/admin/%s/%s";
    protected static final String GET_ACCOUNTS_EXCEPT_THIRD_PARTY = "/account/all";
    protected static final String MI_DATA_URL = "/account/mi-data";
    protected static final String GET_BY_PROVENANCE_ID = "/account/provenance/PI_AAD/%s";
    protected static final String USER_IS_AUTHORISED_FOR_LIST = "/account/isAuthorised/%s/%s/%s";
    protected static final String UPDATE_ACCOUNT = "/account/provenance/PI_AAD/%s";
    protected static final String DELETE_ENDPOINT_V2 = "/account/v2/%s";
    protected static final String UPDATE_ACCOUNT_ROLE = "/account/update/%s/%s";
    protected static final String DELETE_ACCOUNT = "/account/delete/%s";
    protected static final String CREATE_AZURE_ACCOUNT = "/account/add/azure";
    protected static final String GET_AZURE_ACCOUNT_INFO = "/account/azure/%s";
    protected static final String BULK_UPLOAD_URL = "account/media-bulk-upload";
    protected static final String UPDATE_USER_INFO = ACCOUNT_URL + "/provenance";

    // Audit URLs
    protected static final String AUDIT_URL = "/audit";
    protected static final String GET_AUDIT_URL = "/audit/%s";

    // Inactivity URLs
    protected static final String NOTIFY_INACTIVE_MEDIA_ACCOUNT = ACCOUNT_URL + "/media/inactive/notify";
    protected static final String DELETE_INACTIVE_MEDIA_ACCOUNT = ACCOUNT_URL + "/media/inactive";
    protected static final String NOTIFY_INACTIVE_ADMIN_ACCOUNT = ACCOUNT_URL + "/admin/inactive/notify";
    protected static final String DELETE_INACTIVE_ADMIN_ACCOUNT = ACCOUNT_URL + "/admin/inactive";
    protected static final String NOTIFY_INACTIVE_IDAM_ACCOUNT = ACCOUNT_URL + "/idam/inactive/notify";
    protected static final String DELETE_INACTIVE_IDAM_ACCOUNT = ACCOUNT_URL + "/idam/inactive";

    // Media Application URLs
    protected static final String MEDIA_APPLICATION_URL = "/application";
    protected static final String GET_IMAGE_BY_ID = "/application/image/%s";
    protected static final String GET_MEDIA_APPLICATION_URL = "/application/%s";
    protected static final String APPROVE_APPLICATION = "/application/%s/APPROVED";
    protected static final String REJECT_APPLICATION = "/application/%s/REJECTED";
    protected static final String REJECT_APPLICATION_WITH_REASONS = "/application/%s/REJECTED/reasons";
    protected static final String GET_APPLICATIONS_BY_STATUS = "/application/status/PENDING";
    protected static final String REPORTING = "/application/reporting";

    // Subscription URLs
    protected static final String SUBSCRIPTION_URL = "/subscription";
    protected static final String FIND_SUBSCRIPTION_BY_USER_ID_URL = "/subscription/user/";
    protected static final String BUILD_SUBSCRIBER_LIST_URL = SUBSCRIPTION_URL + "/artefact-recipients";
    protected static final String BUILD_DELETED_ARTEFACT_SUBSCRIBER_URL = SUBSCRIPTION_URL + "/deleted-artefact";
    protected static final String CONFIGURE_LIST_TYPE_URL = SUBSCRIPTION_URL + "/configure-list-types/";
    protected static final String ADD_LIST_TYPE_URL = SUBSCRIPTION_URL + "/add-list-types/";
    protected static final String SUBSCRIPTION_BY_LOCATION_URL = SUBSCRIPTION_URL + "/location/";

    //MI URLs
    protected static final String SUBSCRIPTION_MI_DATA_URL = SUBSCRIPTION_URL + "/mi-data-all";
    protected static final String MI_DATA_LOCATION_URL = SUBSCRIPTION_URL + "/mi-data-location";


    protected static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));

    @Value("${test-url}")
    private String testUrl;

    @Value("${data-management-test-url}")
    private String dataManagementUrl;

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = testUrl;
        dataManagementAccessToken = authClient.generateDataManagementAccessToken();
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + authClient.generateAccessToken());
    }

    protected Response doGetRequest(final String path, final Map<String, String> additionalHeaders) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .when()
            .get(path)
            .thenReturn();
    }

    protected Response doGetRequestWithQueryParameters(final String path, final Map<String, String> additionalHeaders,
                                                   final String pageNumber, final String pageSize, final String email) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .queryParam("pageNumber", pageNumber)
            .queryParam("pageSize", pageSize)
            .queryParam("email", email)
            .when()
            .get(path)
            .thenReturn();
    }

    protected Response doGetRequestWithRequestParams(final String path, final Map<String, String> additionalHeaders,
                                                     Map<String, String> params) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .params(params)
            .when()
            .get(path)
            .thenReturn();
    }

    protected Response doPostRequest(final String path, final Map<String, String> additionalHeaders,
                                     final Object body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doPostRequestForB2C(final String path, final Map<String, String> additionalHeaders,
                                     final Map<String, String> issuerId, final String body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .headers(getRequestHeaders(issuerId))
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doPostMultipartForBulk(final String path, final Map<String, String> additionalHeaders,
                                                     final Map<String, String> issuerId, final File mediaList) {
        return given()
            .relaxedHTTPSValidation()
            .headers(additionalHeaders)
            .headers(getRequestHeaders(issuerId))
            .contentType("multipart/form-data")
            .multiPart("mediaList", mediaList)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doPostMultipartForApplication(final String path, final Map<String, String> additionalHeaders,
                                     final File file, String name, String email, String employer, String status) {
        return given()
            .relaxedHTTPSValidation()
            .headers(additionalHeaders)
            .multiPart("file", file)
            .multiPart("fullName", name)
            .multiPart("email", email)
            .multiPart("employer", employer)
            .multiPart("status", status)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doPutRequest(final String path, final Map<String, String> additionalHeaders) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .when()
            .put(path)
            .thenReturn();
    }

    protected Response doPutRequestWithBody(final String path, final Map<String, String> additionalHeaders,
                                            Object body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .body(body)
            .when()
            .put(path)
            .thenReturn();
    }

    protected Response doDeleteRequest(final String path, final Map<String, String> additionalHeaders) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .when()
            .delete(path)
            .thenReturn();
    }

    protected Response doDeleteRequestWithBody(final String path, final Map<String, String> additionalHeaders,
                                               final Object body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .body(body)
            .when()
            .delete(path)
            .thenReturn();
    }

    protected Response doDataManagementPostRequest(final String path, final Map<String, String> additionalHeaders,
                                                   final Object body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .baseUri(dataManagementUrl)
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doDataManagementDeleteRequest(final String path, final Map<String, String> additionalHeaders) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(additionalHeaders))
            .baseUri(dataManagementUrl)
            .when()
            .delete(path)
            .thenReturn();
    }

    private static Map<String, String> getRequestHeaders(final Map<String, String> additionalHeaders) {
        final Map<String, String> headers = new ConcurrentHashMap<>(Map.of(CONTENT_TYPE, CONTENT_TYPE_VALUE));
        if (!CollectionUtils.isEmpty(additionalHeaders)) {
            headers.putAll(additionalHeaders);
        }
        return headers;
    }
}
