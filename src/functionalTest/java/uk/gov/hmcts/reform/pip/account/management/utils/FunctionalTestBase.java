package uk.gov.hmcts.reform.pip.account.management.utils;

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

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FunctionalTestBase {
    protected static final String CONTENT_TYPE_VALUE = "application/json";

    @Autowired
    private OAuthClient authClient;

    protected String accessToken;
    protected String dataManagementAccessToken;

    @Value("${test-url}")
    private String testUrl;

    @Value("${data-management-test-url}")
    private String dataManagementUrl;

    @BeforeAll
    void setUp() {
        RestAssured.baseURI = testUrl;
        accessToken = authClient.generateAccessToken();
        dataManagementAccessToken = authClient.generateDataManagementAccessToken();
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
