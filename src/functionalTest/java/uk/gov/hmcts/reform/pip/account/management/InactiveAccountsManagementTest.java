package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_CTSC;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class InactiveAccountsManagementTest extends AccountHelperBase {
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
    private static final Clock CL = Clock.systemUTC();
    private static final String IDAM_USER_PROVENANCE_ID = UUID.randomUUID().toString();
    private Map<String, String> issuerId;
    private Map<String, String> ctscAdminIssuerId;
    private Map<String, String> systemAdminAuthHeaders;
    private String mediaUserProvenanceId;
    private String adminProvenanceId;
    private String mediaUserId;
    private String adminUserId;
    private String idamUserId;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @BeforeAll
    public void startUp() throws JsonProcessingException {

        String requestBody = """
            {
                "email": "%s",
                "provenanceUserId": "%s"
            }
            """.formatted(TEST_SYSTEM_ADMIN_EMAIL, UUID.randomUUID().toString());

        OBJECT_MAPPER.findAndRegisterModules();

        String userId =  doPostRequest(CREATE_SYSTEM_ADMIN_SSO, bearer, requestBody)
            .jsonPath().getString("userId");
        issuerId = Map.of(REQUESTER_ID_HEADER, userId);

        //ADD SYSTEM ADMIN
        PiUser systemAdminAccount = createSystemAdminAccount(TEST_ADMIN_EMAIL);
        adminProvenanceId = systemAdminAccount.getProvenanceUserId();
        adminUserId = systemAdminAccount.getUserId().toString();

        String ctscAdminId = getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), INTERNAL_ADMIN_CTSC, adminUserId));
        ctscAdminIssuerId = Map.of(REQUESTER_ID_HEADER, ctscAdminId);

        //ADD IDAM USER
        idamUserId = createUser(TEST_IDAM_EMAIL, IDAM_USER_PROVENANCE_ID,
                                Roles.VERIFIED, UserProvenances.CFT_IDAM);

        //ADD MEDIA USER
        AzureAccount mediaUserAzureAccount = createAzureAccount();
        mediaUserProvenanceId = mediaUserAzureAccount.getAzureAccountId();
        mediaUserId = createUser(TEST_EMAIL, mediaUserAzureAccount.getAzureAccountId(),
                                 Roles.VERIFIED, UserProvenances.PI_AAD);

        systemAdminAuthHeaders = new ConcurrentHashMap<>(bearer);
        systemAdminAuthHeaders.putAll(issuerId);

    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL, bearer);
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_ADMIN_EMAIL, bearer);
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_IDAM_EMAIL, bearer);
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_SYSTEM_ADMIN_EMAIL, bearer);
    }

    private AzureAccount createAzureAccount() {
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
            """.formatted(TEST_EMAIL, FIRST_NAME, SURNAME, Roles.VERIFIED, TEST_NAME);


        Response response = doPostRequestForB2C(CREATE_AZURE_ACCOUNT, bearer, ctscAdminIssuerId, requestBody);
        List<AzureAccount> azureAccountList = OBJECT_MAPPER.convertValue(
            response.jsonPath().getJsonObject("CREATED_ACCOUNTS"),
            new TypeReference<>() {
            }
        );
        assertThat(azureAccountList.size()).isEqualTo(1);

        return azureAccountList.getFirst();
    }

    private PiUser createSystemAdminAccount(String email) {
        String requestBody = """
            {
                "email": "%s",
                "firstName": "%s",
                "surname": "%s"
            }
            """.formatted(email, FIRST_NAME, SURNAME);


        Response response = doPostRequestForB2C(ADD_SYSTEM_ADMIN_B2C_URL, bearer, issuerId, requestBody);
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

        Response postResponse = doPostRequestForB2C(CREATE_PI_ACCOUNT, bearer, ctscAdminIssuerId, requestBody);
        List<UUID> piUsersList = OBJECT_MAPPER.convertValue(
            postResponse.jsonPath().getJsonObject("CREATED_ACCOUNTS"),
            new TypeReference<>() {
            }
        );
        assertThat(piUsersList.size()).isEqualTo(1);
        return piUsersList.getFirst().toString();
    }

    void updateUserAccountLastVerifiedDate(String userProvenancesId, UserProvenances userProvenances,
                                           Map<String, String> fieldsToUpdate) {
        JSONObject jsonFieldsToUpdate = new JSONObject(fieldsToUpdate);

        Response response = doPutRequestWithBody(UPDATE_USER_INFO + "/" + userProvenances + "/" + userProvenancesId,
                                                 bearer, jsonFieldsToUpdate.toString());

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
        Response response = doPostRequest(NOTIFY_INACTIVE_MEDIA_ACCOUNT, bearer, "");
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
        Response response = doDeleteRequest(DELETE_INACTIVE_MEDIA_ACCOUNT, bearer);
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());

        final Response getPiUserResponse = doGetRequest(String.format(GET_BY_USER_ID, mediaUserId),
                                                        systemAdminAuthHeaders);
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
        Response response = doPostRequest(NOTIFY_INACTIVE_ADMIN_ACCOUNT, bearer, "");
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
        Response response = doDeleteRequest(DELETE_INACTIVE_ADMIN_ACCOUNT, bearer);
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());
        final Response getPiUserResponse = doGetRequest(String.format(GET_BY_USER_ID, adminUserId),
                                                        systemAdminAuthHeaders);
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
        Response response = doPostRequest(NOTIFY_INACTIVE_IDAM_ACCOUNT, bearer, "");
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
        Response response = doDeleteRequest(DELETE_INACTIVE_IDAM_ACCOUNT, bearer);
        assertThat(response.getStatusCode()).isEqualTo(NO_CONTENT.value());
        final Response getPiUserResponse = doGetRequest(String.format(GET_BY_USER_ID, idamUserId),
                                                        systemAdminAuthHeaders);
        assertThat(getPiUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

}
