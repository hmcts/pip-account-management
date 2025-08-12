package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

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

class AccountTest extends AccountHelperBase {
    private static final String ADMIN_ID = "x-admin-id";
    private static final String SUPER_ADMIN_CTSC_ROLE_NAME = "INTERNAL_SUPER_ADMIN_CTSC";
    private static final String SUPER_ADMIN_LOCAL_ROLE_NAME = "INTERNAL_SUPER_ADMIN_LOCAL";

    Map<Roles, String> idMap = new ConcurrentHashMap<>();

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
        idMap.put(Roles.SYSTEM_ADMIN, createSystemAdminAccount().getUserId());

        idMap.put(Roles.INTERNAL_SUPER_ADMIN_CTSC, getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(),
                          Roles.INTERNAL_SUPER_ADMIN_CTSC, UserProvenances.SSO)));

        idMap.put(Roles.INTERNAL_SUPER_ADMIN_LOCAL, getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(),
                          Roles.INTERNAL_SUPER_ADMIN_LOCAL, UserProvenances.SSO)));

        idMap.put(Roles.INTERNAL_ADMIN_LOCAL, getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(),
                          Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.SSO)));
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, bearer);
        doDeleteRequest(String.format(DELETE_ACCOUNT, thirdPartyUserId), bearer);
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
        Response createdResponse = createVerifiedAccount(email, provenanceId);
        assertThat(createdResponse.getBody().as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).size())
            .isEqualTo(1);
    }

    @Test
    void shouldNotBeAbleToCreateThirdPartyAccountIfNotSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        final Response createResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());
    }

    @Test
    void shouldBeAbleToCreateThirdPartyAccountIfSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, idMap.get(Roles.SYSTEM_ADMIN));

        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createdResponse.getStatusCode()).isEqualTo(CREATED.value());

        thirdPartyUserId = (String) createdResponse.getBody()
            .as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).get(0);
    }

    @Test
    void shouldBeAbleToGetAnAccountByUserId() throws Exception {
        String createdUserId = getCreatedAccountUserId(createVerifiedAccount(email, provenanceId));

        Response getUserResponse = doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(PiUser.class)).matches(user -> user.getEmail().equals(email));
    }

    @Test
    void shouldBeAbleToGetAnAccountByProvenanceId() throws Exception {
        createVerifiedAccount(email, provenanceId);

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(PiUser.class)).matches(user -> user.getEmail().equals(email));
    }

    @ParameterizedTest
    @CsvSource({
        "CIVIL_DAILY_CAUSE_LIST,PUBLIC,true",
        "CIVIL_DAILY_CAUSE_LIST,PRIVATE,true",
        "SJP_PUBLIC_LIST,CLASSIFIED,true",
        "CIVIL_DAILY_CAUSE_LIST,CLASSIFIED,false"
    })
    void checkIfUserIsAuthorised(String listType, String sensitivity, boolean shouldBeAuthorised) throws Exception {
        String createdUserId = getCreatedAccountUserId(createVerifiedAccount(email, provenanceId));

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              listType, sensitivity), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isEqualTo(shouldBeAuthorised);
    }

    @Test
    void shouldBeAbleToUpdateAccount() throws Exception {
        createVerifiedAccount(email, provenanceId);

        Map<String, String> updateParams = new ConcurrentHashMap<>();
        updateParams.put("lastVerifiedDate", "2024-12-01T01:01:01.123456Z");
        updateParams.put("lastSignedInDate", "2024-12-02T01:01:01.123456Z");

        Response updateResponse = doPutRequestWithBody(String.format(UPDATE_ACCOUNT, provenanceId), bearer,
                                                       objectMapper.writeValueAsString(updateParams));

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        PiUser user = getUserResponse.getBody().as(PiUser.class);

        assertThat(user.getLastVerifiedDate()).isEqualTo(LocalDateTime.parse("2024-12-01T01:01:01"));
        assertThat(user.getLastSignedInDate()).isEqualTo(LocalDateTime.parse("2024-12-02T01:01:01"));
    }

    @Test
    void shouldBeAbleToDeleteAccount() throws Exception {
        String createdUserId = getCreatedAccountUserId(createVerifiedAccount(email, provenanceId));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ACCOUNT, createdUserId), bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @ParameterizedTest
    @CsvSource({
        "PI_AAD,VERIFIED",
        "SSO,INTERNAL_ADMIN_LOCAL",
    })
    void shouldBeAbleToDeleteAccountV2(String provenance, String createdUserRole)
        throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId,
                                                                     Roles.valueOf(createdUserRole),
                                                                     UserProvenances.valueOf(provenance)));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, idMap.get(Roles.SYSTEM_ADMIN));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountV2WhenSystemAdminAndThirdParty() throws JsonProcessingException {
        List<PiUser> thirdParty = generateThirdParty();
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, idMap.get(Roles.SYSTEM_ADMIN));

        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                       headers, objectMapper.writeValueAsString(thirdParty));
        thirdPartyUserId = (String) createdResponse.getBody()
            .as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        headers.put(ADMIN_ID, idMap.get(Roles.SYSTEM_ADMIN));
        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, thirdPartyUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldNotBeAbleToDeleteThirdPartyAccountWhenLocalAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, idMap.get(Roles.SYSTEM_ADMIN));
        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                       headers, objectMapper.writeValueAsString(thirdParty));
        thirdPartyUserId = (String) createdResponse.getBody()
            .as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, idMap.get(Roles.INTERNAL_ADMIN_LOCAL));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, thirdPartyUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_USER_ID, thirdPartyUserId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToDeleteVerifiedAccountWhenNonSystemAdmin()
        throws Exception {
        String createdUserId = getCreatedAccountUserId(createVerifiedAccount(email, provenanceId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToDeleteAccountWhenUserNotProvidedAndNotSso() throws Exception {
        String createdUserId = getCreatedAccountUserId(createVerifiedAccount(email, provenanceId));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountWhenUserNotProvidedAndSso() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId,
                                                                     Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.SSO));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldNotBeAbleToUpdateTheirOwnAccount() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC),
                          SUPER_ADMIN_LOCAL_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)), bearer)
                .getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleWhenUserNotProvidedAndRoleIsNotSso() throws JsonProcessingException {
        List<PiUser> thirdParty = generateThirdParty();
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, idMap.get(Roles.SYSTEM_ADMIN));
        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                       headers, objectMapper.writeValueAsString(thirdParty));
        thirdPartyUserId = (String) createdResponse.getBody()
            .as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, thirdPartyUserId, Roles.VERIFIED_THIRD_PARTY_CFT), bearer);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)),
                bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @ParameterizedTest
    @CsvSource({
        "INTERNAL_SUPER_ADMIN_CTSC",
        "INTERNAL_SUPER_ADMIN_LOCAL",
        "SYSTEM_ADMIN",
        "INTERNAL_ADMIN_LOCAL"
    })
    void shouldNotBeAbleToUpdateRoleIfAdminAndNotThirdParty(String role) throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.SSO));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, idMap.get(Roles.valueOf(role)));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());
    }

    @Test
    void shouldNotBeAbleToUpdateThirdPartyRoleIfInternalAdminLocal() throws JsonProcessingException {
        List<PiUser> thirdParty = generateThirdParty();
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, idMap.get(Roles.SYSTEM_ADMIN));
        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                       headers, objectMapper.writeValueAsString(thirdParty));
        thirdPartyUserId = (String) createdResponse.getBody()
            .as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, idMap.get(Roles.INTERNAL_ADMIN_LOCAL));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, thirdPartyUserId, Roles.VERIFIED_THIRD_PARTY_CFT), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, thirdPartyUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.GENERAL_THIRD_PARTY);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleIfVerifiedAccount() throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(createVerifiedAccount(email, provenanceId));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ADMIN_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.VERIFIED);
    }

    @Test
    void shouldBeAbleToUpdateRoleIfSsoAndNoAdminProvided() throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.SSO));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), bearer);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

}
