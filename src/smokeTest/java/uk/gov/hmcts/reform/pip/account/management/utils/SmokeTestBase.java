package uk.gov.hmcts.reform.pip.account.management.utils;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import static io.restassured.RestAssured.given;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;

@SpringBootTest(classes = {OAuthClient.class}, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SmokeTestBase {
    private static final String TESTING_SUPPORT_LOCATION_URL = "/testing-support/location/";

    private String accessToken;
    private String dataManagementAccessToken;

    @Value("${test-url}")
    private String testUrl;

    @Value("${data-management-test-url}")
    private String dataManagementUrl;

    @Autowired
    private OAuthClient authClient;

    @BeforeAll
    void startup() {
        RestAssured.baseURI = testUrl;
        accessToken = authClient.generateAccessToken();
        dataManagementAccessToken = authClient.generateDataManagementAccessToken();
    }

    protected Response doGetRequest(final String path) {
        return given()
            .relaxedHTTPSValidation()
            .when()
            .get(path)
            .thenReturn();
    }

    protected Response doPostRequest(final String path, final Map<String, String> additionalHeaders,
                                     final Object body) {
        return given()
            .relaxedHTTPSValidation()
            .headers(Map.of(AUTHORIZATION, "bearer " + accessToken))
            .headers(getRequestHeaders(additionalHeaders))
            .body(body)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doPostMultipartForApplication(String path, String userId, File file, String name, String email,
                                                     String employer, String status) {
        return given()
            .relaxedHTTPSValidation()
            .headers(Map.of(AUTHORIZATION, "bearer " + accessToken))
            .header("x-requester-id", userId)
            .multiPart("file", file)
            .multiPart("fullName", name)
            .multiPart("email", email)
            .multiPart("employer", employer)
            .multiPart("status", status)
            .when()
            .post(path)
            .thenReturn();
    }

    protected Response doDeleteRequest(final String path) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(Collections.emptyMap()))
            .when()
            .delete(path)
            .thenReturn();
    }

    protected Response createTestLocation(String locationId, String locationName) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(Map.of(AUTHORIZATION, "Bearer " + dataManagementAccessToken)))
            .baseUri(dataManagementUrl)
            .body(locationName)
            .when()
            .post(TESTING_SUPPORT_LOCATION_URL + "/" + locationId)
            .thenReturn();
    }

    protected Response deleteTestLocation(String locationName) {
        return given()
            .relaxedHTTPSValidation()
            .headers(getRequestHeaders(Map.of(AUTHORIZATION, "Bearer " + dataManagementAccessToken)))
            .baseUri(dataManagementUrl)
            .when()
            .delete(TESTING_SUPPORT_LOCATION_URL + locationName)
            .thenReturn();
    }

    private Map<String, String> getRequestHeaders(final Map<String, String> additionalHeaders) {
        final Map<String, String> headers = new ConcurrentHashMap<>();
        headers.put(AUTHORIZATION, "bearer " + accessToken);
        headers.put(CONTENT_TYPE, "application/json");

        if (!CollectionUtils.isEmpty(additionalHeaders)) {
            headers.putAll(additionalHeaders);
        }
        return headers;
    }

    protected static String createRandomId() {
        Integer randomNumber = 10_000 + new Random(System.currentTimeMillis()).nextInt(20_000);
        return randomNumber.toString();
    }
}
