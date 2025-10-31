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
            createAccount(generateEmail(), UUID.randomUUID().toString(), Roles.INTERNAL_SUPER_ADMIN_CTSC,
                          idMap.get(Roles.SYSTEM_ADMIN))));

        idMap.put(Roles.INTERNAL_SUPER_ADMIN_LOCAL, getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), Roles.INTERNAL_SUPER_ADMIN_LOCAL,
                          idMap.get(Roles.SYSTEM_ADMIN))));

        idMap.put(Roles.INTERNAL_ADMIN_LOCAL, getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), Roles.INTERNAL_ADMIN_LOCAL,
                          idMap.get(Roles.SYSTEM_ADMIN))));
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
        Response createdResponse = createAccount(email, provenanceId, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));
        assertThat(createdResponse.getBody().as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).size())
            .isEqualTo(1);
    }

    @Test
    void shouldBeAbleToCreateNonPiAadAccountIfNoUserIdProvided() throws Exception {
        PiUser piUser = new PiUser();
        piUser.setEmail(email);
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setForenames("TEST");
        piUser.setSurname("USER");
        piUser.setUserProvenance(UserProvenances.SSO);
        piUser.setProvenanceUserId(provenanceId);
        List<PiUser> users = List.of(piUser);
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);

        Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(users));

        assertThat(createdResponse.getBody().as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).size())
            .isEqualTo(1);
    }

    @Test
    void shouldNotBeAbleToCreateThirdPartyAccountIfNotSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        final Response createResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());
    }

    @Test
    void shouldBeAbleToCreateThirdPartyAccountIfSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));

        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createdResponse.getStatusCode()).isEqualTo(CREATED.value());

        thirdPartyUserId = (String) createdResponse.getBody()
            .as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).getFirst();
    }

    @Test
    void shouldBeAbleToGetAnAccountByUserId() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId,
                                                                     idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));
        Response getUserResponse = doGetRequest(String.format(GET_BY_USER_ID, createdUserId), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(PiUser.class)).matches(user -> user.getEmail().equals(email));
    }

    @Test
    void shouldBeAbleToGetAnAccountByProvenanceId() throws Exception {
        createAccount(email, provenanceId, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));
        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);

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
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId,
                                                                     idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));
        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              listType, sensitivity), headers);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isEqualTo(shouldBeAuthorised);
    }

    @Test
    void shouldBeAbleToUpdateAccount() throws Exception {
        createAccount(email, provenanceId, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        Map<String, String> updateParams = new ConcurrentHashMap<>();
        updateParams.put("lastVerifiedDate", "2024-12-01T01:01:01.123456Z");
        updateParams.put("lastSignedInDate", "2024-12-02T01:01:01.123456Z");

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));
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
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId,
                                                                     idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));
        Response deleteResponse = doDeleteRequest(String.format(DELETE_ACCOUNT, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), headers);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountV2() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId,
                idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));
        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @ParameterizedTest
    @CsvSource({
        "INTERNAL_ADMIN_LOCAL,INTERNAL_ADMIN_LOCAL",
        "INTERNAL_SUPER_ADMIN_CTSC,INTERNAL_ADMIN_LOCAL",
        "INTERNAL_SUPER_ADMIN_CTSC,VERIFIED"
    })
    void shouldNotBeAbleToDeleteAccount(String adminRole, String createdRole) throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId, Roles.valueOf(createdRole),
                                                                     idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)));

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.valueOf(adminRole)));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToDeleteAccountWhenUserNotProvided() throws Exception {
        String createdUserId = getCreatedAccountUserId(createAccount(email, provenanceId,
                                                                     idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)));

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToUpdateTheirOwnAccount() {
        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC),
                          SUPER_ADMIN_LOCAL_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Map<String, String> getUserHeaders = new ConcurrentHashMap<>(bearer);
        getUserHeaders.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)), getUserHeaders)
                .getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleWhenUserNotProvided() {
        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC),
                SUPER_ADMIN_LOCAL_ROLE_NAME), bearer);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Map<String, String> getUserHeaders = new ConcurrentHashMap<>(bearer);
        getUserHeaders.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC)), getUserHeaders)
                    .getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @ParameterizedTest
    @CsvSource({
        "INTERNAL_SUPER_ADMIN_CTSC, PI_AAD",
        "INTERNAL_SUPER_ADMIN_LOCAL, PI_AAD",
        "SYSTEM_ADMIN, PI_AAD",
        "INTERNAL_ADMIN_LOCAL, SSO"
    })
    void shouldBeAbleToUpdateRole(String role, String userProvenance) throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.valueOf(userProvenance),
                          idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC))
        );

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.valueOf(role)));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        Map<String, String> getUserHeaders = new ConcurrentHashMap<>(bearer);
        getUserHeaders.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));

        PiUser getUserResponse = doGetRequest(String.format(GET_BY_USER_ID, createdUserId), getUserHeaders)
                .getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @ParameterizedTest
    @CsvSource({
        "INTERNAL_ADMIN_LOCAL,INTERNAL_ADMIN_LOCAL",
        "INTERNAL_SUPER_ADMIN_LOCAL,VERIFIED"
    })
    void shouldNotBeAbleToUpdateRole(String adminRole, String createdRole) throws JsonProcessingException {
        String createdUserId = getCreatedAccountUserId(
            createAccount(email, provenanceId, Roles.valueOf(createdRole), idMap.get(Roles.INTERNAL_SUPER_ADMIN_CTSC))
        );

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, idMap.get(Roles.valueOf(adminRole)));

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE, createdUserId, SUPER_ADMIN_CTSC_ROLE_NAME), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Map<String, String> getUserHeaders = new ConcurrentHashMap<>(bearer);
        getUserHeaders.put(REQUESTER_ID_HEADER, idMap.get(Roles.SYSTEM_ADMIN));

        PiUser getUserResponse = doGetRequest(String.format(GET_BY_USER_ID, createdUserId), getUserHeaders)
                .getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.valueOf(createdRole));
    }
}
