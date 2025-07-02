package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_ADMIN_LOCAL;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_SUPER_ADMIN_CTSC;
import static uk.gov.hmcts.reform.pip.model.account.Roles.INTERNAL_SUPER_ADMIN_LOCAL;

class AccountTest extends AccountHelperBase {

    private static final String GET_BY_PROVENANCE_ID = "/account/provenance/PI_AAD/%s";
    private static final String GET_BY_USER_ID = "/account/%s";
    private static final String USER_IS_AUTHORISED_FOR_LIST = "/account/isAuthorised/%s/%s/%s";
    private static final String UPDATE_ACCOUNT = "/account/provenance/PI_AAD/%s";
    private static final String DELETE_ENDPOINT = "/account/delete/%s";
    private static final String DELETE_ENDPOINT_V2 = "/account/v2/%s";
    private static final String UPDATE_ACCOUNT_ROLE = "/account/update/%s/%s";
    private static final String ADMIN_ID = "x-admin-id";
    private static final String SUPER_ADMIN_CTSC_ROLE_NAME = "INTERNAL_SUPER_ADMIN_CTSC";
    private static final String SUPER_ADMIN_LOCAL_ROLE_NAME = "INTERNAL_SUPER_ADMIN_LOCAL";

    private String systemAdminId;
    private String ctscSuperAdminId;
    private String localSuperAdminId;
    private String localAdminId;
    private String thirdPartyUserId;

    private String email;
    private String provenanceId;

    @BeforeEach
    public void setupRandomVariables() {
        email = generateEmail();
        provenanceId = UUID.randomUUID().toString();
    }

    @BeforeAll
    public void startUp() throws JsonProcessingException {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        PiUser systemAdminUser = createSystemAdminAccount();
        systemAdminId = systemAdminUser.getUserId();

        ctscSuperAdminId = getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), INTERNAL_SUPER_ADMIN_CTSC, systemAdminId));

        localSuperAdminId = getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), INTERNAL_SUPER_ADMIN_LOCAL, systemAdminId));

        localAdminId = getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), INTERNAL_ADMIN_LOCAL, systemAdminId));
    }

    @AfterAll
    public void teardown() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, headers);
        doDeleteRequest(String.format(DELETE_ENDPOINT, thirdPartyUserId), headers);
    }

    private List<PiUser> generateThirdParty() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.GENERAL_THIRD_PARTY);
        piUser.setUserProvenance(UserProvenances.THIRD_PARTY);
        piUser.setProvenanceUserId(generateEmail());

        List<PiUser> users = new ArrayList<>();
        users.add(piUser);

        return users;
    }

    @Test
    void shouldBeAbleToCreateAccount() throws Exception {
        Response createdResponse = createAccount(email, provenanceId, systemAdminId);
        assertThat(createdResponse.getBody().as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).size())
            .isEqualTo(1);
    }

    @Test
    void shouldNotBeAbleToCreateThirdPartyAccountIfNotSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, ctscSuperAdminId);

        final Response createResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());
    }

    @Test
    void shouldBeAbleToCreateThirdPartyAccountIfSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createdResponse.getStatusCode()).isEqualTo(CREATED.value());

        thirdPartyUserId = (String) createdResponse.getBody()
            .as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).getFirst();
    }

    @Test
    void shouldBeAbleToGetAnAccountByUserId() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Response getUserResponse = doGetRequest(String.format(GET_BY_USER_ID, createdUserId), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(PiUser.class)).matches(user -> user.getEmail().equals(email));
    }

    @Test
    void shouldBeAbleToGetAnAccountByProvenanceId() throws Exception {
        createAccount(email, provenanceId, systemAdminId);

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(PiUser.class)).matches(user -> user.getEmail().equals(email));
    }

    @Test
    void checkIfUserIsAuthorisedWhenPublicList() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC
        ), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isTrue();
    }

    @Test
    void checkIfUserIsAuthorisedWhenPrivateList() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PRIVATE
        ), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isTrue();
    }

    @Test
    void checkIfUserIsAuthorisedWhenClassifiedList() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.SJP_PUBLIC_LIST, Sensitivity.CLASSIFIED
        ), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isTrue();
    }

    @Test
    void checkIfUserIsNotAuthorisedWhenClassifiedList() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.CLASSIFIED
        ), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isFalse();
    }

    @Test
    void shouldBeAbleToUpdateAccount() throws Exception {
        createAccount(email, provenanceId, systemAdminId);
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Map<String, String> updateParams = new ConcurrentHashMap<>();
        updateParams.put("lastVerifiedDate", "2024-12-01T01:01:01.123456Z");
        updateParams.put("lastSignedInDate", "2024-12-02T01:01:01.123456Z");

        Response updateResponse = doPutRequestWithBody(String.format(UPDATE_ACCOUNT, provenanceId), headers,
                                                       objectMapper.writeValueAsString(updateParams));

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);
        PiUser user = getUserResponse.getBody().as(PiUser.class);

        assertThat(user.getLastVerifiedDate()).isEqualTo(LocalDateTime.parse("2024-12-01T01:01:01"));
        assertThat(user.getLastSignedInDate()).isEqualTo(LocalDateTime.parse("2024-12-02T01:01:01"));
    }

    @Test
    void shouldBeAbleToDeleteAccount() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountV2WhenSystemAdmin() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, systemAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {SUPER_ADMIN_CTSC_ROLE_NAME, SUPER_ADMIN_LOCAL_ROLE_NAME})
    void shouldNotBeAbleToDeleteAccountV2WhenSuperAdmin(Roles role) throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, INTERNAL_ADMIN_LOCAL,
                                                                     systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, role == INTERNAL_SUPER_ADMIN_CTSC ? ctscSuperAdminId : localSuperAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToDeleteAccountWhenLocalAdmin() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, INTERNAL_ADMIN_LOCAL,
                                                                     systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, localAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToDeleteVerifiedAccountV2WhenNonSystemAdmin() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, ctscSuperAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToDeleteAccountWhenUserNotProvided() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToUpdateTheirOwnAccount() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, ctscSuperAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, ctscSuperAdminId, SUPER_ADMIN_LOCAL_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, ctscSuperAdminId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleWhenUserNotProvided() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, systemAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, ctscSuperAdminId, SUPER_ADMIN_LOCAL_ROLE_NAME), bearer);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, ctscSuperAdminId), headers).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldBeAbleToUpdateRoleIfSystemAdmin() throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, INTERNAL_ADMIN_LOCAL, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, systemAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(INTERNAL_SUPER_ADMIN_CTSC);
    }

    @ParameterizedTest
    @EnumSource(value = Roles.class, names = {SUPER_ADMIN_CTSC_ROLE_NAME, SUPER_ADMIN_LOCAL_ROLE_NAME})
    void shouldBeAbleToUpdateRoleIfSuperAdmin(Roles role) throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, INTERNAL_ADMIN_LOCAL, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, role == INTERNAL_SUPER_ADMIN_CTSC ? ctscSuperAdminId : localSuperAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleIfNotSuperAdmin() throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, INTERNAL_ADMIN_LOCAL, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, localAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(INTERNAL_ADMIN_LOCAL);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleIfVerifiedAccount() throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, localSuperAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.VERIFIED);
    }

    @Test
    void shouldBeAbleToUpdateRoleIfSso() throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, INTERNAL_ADMIN_LOCAL, UserProvenances.SSO, systemAdminId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, localAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(INTERNAL_SUPER_ADMIN_CTSC);
    }

}
