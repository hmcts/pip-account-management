package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings("PMD.TooManyMethods")
class InactiveAccountsManagementTest extends FunctionalTestBase {
    private static final String TEST_NAME = "E2E Account Management Test Name";
    private static final Integer TEST_EMAIL_RANDOM_NUMBER =
        ThreadLocalRandom.current().nextInt(1000, 9999);
    private static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", TEST_EMAIL_RANDOM_NUMBER);
    private static final String TEST_ADMIN_EMAIL_PREFIX = String.format(
        "pip-am-test-admin-email-%s", TEST_EMAIL_RANDOM_NUMBER);
    private static final String TEST_IDAM_EMAIL_PREFIX = String.format(
        "pip-am-test-idam-email-%s", TEST_EMAIL_RANDOM_NUMBER);
    private static final String TEST_SYSTEM_ADMIN_EMAIL_PREFIX = String.format(
        "pip-am-test-system-admin-email-%s", TEST_EMAIL_RANDOM_NUMBER);
    private static final String EMAIL_DOMAIN = "%s@justice.gov.uk";
    private static final String TEST_EMAIL =
        String.format(EMAIL_DOMAIN, TEST_EMAIL_PREFIX);
    private static final String TEST_ADMIN_EMAIL =
        String.format(EMAIL_DOMAIN, TEST_ADMIN_EMAIL_PREFIX);
    private static final String TEST_IDAM_EMAIL =
        String.format(EMAIL_DOMAIN, TEST_IDAM_EMAIL_PREFIX);
    private static final String TEST_SYSTEM_ADMIN_EMAIL = TEST_SYSTEM_ADMIN_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String FIRST_NAME = "E2E Account Management";
    private static final String SURNAME = "Test Name";
    private static final String LAST_SINGED_IN_DATE = "lastSignedInDate";
    private static final String BEARER = "Bearer ";
    private static final String ISSUER_ID = "x-issuer-id";
    private static final Clock CL = Clock.systemUTC();
    private static final String IDAM_USER_PROVENANCE_ID = UUID.randomUUID().toString();
    private Map<String, String> headers;
    private Map<String, String> issuerId;
    private String mediaUserProvenanceId;
    private String adminProvenanceId;
    private String mediaUserId;
    private String adminUserId;
    private String idamUserId;
    private static final String ACCOUNT_URL = "/account";
    private static final String ADD_USER_B2C_URL = ACCOUNT_URL + "/add/azure";
    private static final String ADD_PI_USER_URL = ACCOUNT_URL + "/add/pi";
    private static final String UPDATE_USER_INFO = ACCOUNT_URL + "/provenance";
    private static final String NOTIFY_INACTIVE_MEDIA_ACCOUNT = ACCOUNT_URL + "/media/inactive/notify";
    private static final String DELETE_INACTIVE_MEDIA_ACCOUNT = ACCOUNT_URL + "/media/inactive";
    private static final String NOTIFY_INACTIVE_ADMIN_ACCOUNT = ACCOUNT_URL + "/admin/inactive/notify";
    private static final String DELETE_INACTIVE_ADMIN_ACCOUNT = ACCOUNT_URL + "/admin/inactive";
    private static final String NOTIFY_INACTIVE_IDAM_ACCOUNT = ACCOUNT_URL + "/idam/inactive/notify";
    private static final String DELETE_INACTIVE_IDAM_ACCOUNT = ACCOUNT_URL + "/idam/inactive";
    private static final String ADD_SYSTEM_ADMIN_B2C_URL = ACCOUNT_URL + "/add/system-admin";
    private static final String TESTING_SUPPORT_ACCOUNT_URL = "/testing-support/account/";
    private static final String GET_PI_USER_URL = "/account/%s";
    private static final String SYSTEM_ADMIN_SSO_URL = ACCOUNT_URL + "/system-admin";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeAll
    public void startUp() {

        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_SYSTEM_ADMIN_EMAIL, UUID.randomUUID().toString());

        OBJECT_MAPPER.findAndRegisterModules();

        headers = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
        String userId =  doPostRequest(SYSTEM_ADMIN_SSO_URL, headers, requestBody)
            .jsonPath().getString("userId");
        issuerId = Map.of(ISSUER_ID, userId);

        //ADD MEDIA USER
        AzureAccount mediaUserAzureAccount = createAzureAccount(TEST_EMAIL, Roles.VERIFIED);
        mediaUserProvenanceId = mediaUserAzureAccount.getAzureAccountId();
        mediaUserId = createUser(TEST_EMAIL, mediaUserAzureAccount.getAzureAccountId(),
                                   Roles.VERIFIED, UserProvenances.PI_AAD);

        //ADD SYSTEM ADMIN
        PiUser systemAdminAccount = createSystemAdminAccount(TEST_ADMIN_EMAIL);
        adminProvenanceId = systemAdminAccount.getProvenanceUserId();
        adminUserId = systemAdminAccount.getUserId().toString();

        //ADD IDAM USER
        idamUserId = createUser(TEST_IDAM_EMAIL, IDAM_USER_PROVENANCE_ID,
                   Roles.VERIFIED, UserProvenances.CFT_IDAM);
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_EMAIL, headers);
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_ADMIN_EMAIL, headers);
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_IDAM_EMAIL, headers);
        doDeleteRequest(TESTING_SUPPORT_ACCOUNT_URL + TEST_SYSTEM_ADMIN_EMAIL, headers);
    }

    private AzureAccount createAzureAccount(String email, Roles role) {
        String requestBody = """
            [
                {
                    "email": "%s",
                    "firstName": "%s",
                    "surname": "%s",
                    "role": "%s",
                    "displayName": "%s"
                }
            ]
            """.formatted(email, FIRST_NAME, SURNAME, role, TEST_NAME);


        Response response = doPostRequestForB2C(ADD_USER_B2C_URL, headers, issuerId, requestBody);
        List<AzureAccount> azureAccountList = OBJECT_MAPPER.convertValue(
            response.jsonPath().getJsonObject("CREATED_ACCOUNTS"),
            new TypeReference<>() {
            }
        );
        assertThat(azureAccountList.size()).isEqualTo(1);

        return azureAccountList.get(0);
    }

    private PiUser createSystemAdminAccount(String email) {
        String requestBody = """
            {
                "email": "%s",
                "firstName": "%s",
                "surname": "%s"
            }
            """.formatted(email, FIRST_NAME, SURNAME);


        Response response = doPostRequestForB2C(ADD_SYSTEM_ADMIN_B2C_URL, headers, issuerId, requestBody);
        PiUser piUser = response.getBody().as(PiUser.class);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        return piUser;
    }

    private String createUser(String email,
                              String provenancesId, Roles role, UserProvenances provenances) {
        String requestBody = """
            [
                {
                    "email": "%s",
                    "provenanceUserId": "%s",
                    "roles": "%s",
                    "userProvenance": "%s"
                }
            ]
            """.formatted(email, provenancesId, role, provenances);

        Response postResponse = doPostRequestForB2C(ADD_PI_USER_URL, headers, issuerId, requestBody);
        List<UUID> piUsersList = OBJECT_MAPPER.convertValue(
            postResponse.jsonPath().getJsonObject("CREATED_ACCOUNTS"),
            new TypeReference<>() {
            }
        );
        assertThat(piUsersList.size()).isEqualTo(1);
        return piUsersList.get(0).toString();
    }

    void updateUserAccountLastVerifiedDate(String userProvenancesId, UserProvenances userProvenances,
                                           Map<String, String> fieldsToUpdate) {
        JSONObject jsonFieldsToUpdate = new JSONObject(fieldsToUpdate);

        Response response = doPutRequestWithJsonBody(UPDATE_USER_INFO + "/" + userProvenances + "/" + userProvenancesId,
                                              headers, jsonFieldsToUpdate.toString());

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    @Order(1)
    void shouldBeAbleToNotifyInactiveMediaAccounts() {
        ZonedDateTime localDateTime = ZonedDateTime.now(CL).minusDays(350);
        Map<String, String> updateParameters = Map.of(
            "lastVerifiedDate", localDateTime.toString()
        );
        updateUserAccountLastVerifiedDate(mediaUserProvenanceId,
                                          UserProvenances.PI_AAD, updateParameters);
        Response response = doPostRequest(NOTIFY_INACTIVE_MEDIA_ACCOUNT, headers, "");
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    @Order(2)
    void shouldBeAbleToDeleteInactiveMediaAccounts() {
        ZonedDateTime localDateTime = ZonedDateTime.now(CL).minusDays(365);
        Map<String, String> updateParameters = Map.of(
            "lastVerifiedDate", localDateTime.toString()
        );
        updateUserAccountLastVerifiedDate(mediaUserProvenanceId,
                                          UserProvenances.PI_AAD, updateParameters);
        Response response = doDeleteRequest(DELETE_INACTIVE_MEDIA_ACCOUNT, headers);
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());

        final Response getPiUserResponse = doGetRequest(String.format(GET_PI_USER_URL, mediaUserId),
                                                             headers);
        assertThat(getPiUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    @Order(3)
    void shouldBeAbleToNotifyInactiveAdminAccounts() {
        ZonedDateTime localDateTime = ZonedDateTime.now(CL).minusDays(76);
        Map<String, String> updateParameters = Map.of(
            LAST_SINGED_IN_DATE, localDateTime.toString()
        );
        updateUserAccountLastVerifiedDate(adminProvenanceId,
                                          UserProvenances.PI_AAD, updateParameters);
        Response response = doPostRequest(NOTIFY_INACTIVE_ADMIN_ACCOUNT, headers, "");
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    @Order(4)
    void shouldBeAbleToDeleteInactiveAdminAccounts() {
        ZonedDateTime localDateTime = ZonedDateTime.now(CL).minusDays(90);
        Map<String, String> updateParameters = Map.of(
            LAST_SINGED_IN_DATE, localDateTime.toString()
        );
        updateUserAccountLastVerifiedDate(adminProvenanceId,
                                          UserProvenances.PI_AAD, updateParameters);
        Response response = doDeleteRequest(DELETE_INACTIVE_ADMIN_ACCOUNT, headers);
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());
        final Response getPiUserResponse = doGetRequest(String.format(GET_PI_USER_URL, adminUserId),
                                                        headers);
        assertThat(getPiUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    @Order(5)
    void shouldBeAbleToNotifyInactiveIdamAccounts() {
        ZonedDateTime localDateTime = ZonedDateTime.now(CL).minusDays(118);
        Map<String, String> updateParameters = Map.of(
            LAST_SINGED_IN_DATE, localDateTime.toString()
        );
        updateUserAccountLastVerifiedDate(IDAM_USER_PROVENANCE_ID,
                                          UserProvenances.CFT_IDAM, updateParameters);
        Response response = doPostRequest(NOTIFY_INACTIVE_IDAM_ACCOUNT, headers, "");
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());
    }

    @Test
    @Order(6)
    void shouldBeAbleToDeleteInactiveIdamAccounts() {
        ZonedDateTime localDateTime = ZonedDateTime.now(CL).minusDays(132);
        Map<String, String> updateParameters = Map.of(
            LAST_SINGED_IN_DATE, localDateTime.toString()
        );
        updateUserAccountLastVerifiedDate(IDAM_USER_PROVENANCE_ID,
                                          UserProvenances.CFT_IDAM, updateParameters);
        Response response = doDeleteRequest(DELETE_INACTIVE_IDAM_ACCOUNT, headers);
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());
        final Response getPiUserResponse = doGetRequest(String.format(GET_PI_USER_URL, idamUserId),
                                                        headers);
        assertThat(getPiUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

}
