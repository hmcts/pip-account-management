package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
public class PiAccountControllerTest extends FunctionalTestBase {

    private static final String BEARER = "Bearer ";
    private static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TESTING_SUPPORT_DELETE_ACCOUNT_URL = "/testing-support/account/";
    private static final String CREATE_PI_ACCOUNT_PATH = "/account/add/pi";
    private static final String GET_BY_PROVENANCE_ID = "/account/provenance/PI_AAD/%s";
    private static final String GET_BY_USER_ID = "/account/%s";
    private static final String USER_IS_AUTHORISED_FOR_LIST = "/account/isAuthorised/%s/%s/%s";
    private static final String EMAILS_ENDPOINT = "/account/emails";
    private static final String UPDATE_ACCOUNT_ENDPOINT = "/account/provenance/PI_AAD/%s";
    private static final String DELETE_ENDPOINT = "/account/delete/%s";
    private static final String DELETE_ENDPOINT_V2 = "/account/v2/%s";
    private static final String CREATE_SYSTEM_ADMIN_SSO = "/account/system-admin";
    private static final String UPDATE_ACCOUNT_ROLE_ENDPOINT = "/account/update/%s/%s";
    private static final String ISSUER_ID = "x-issuer-id";
    private static final String ADMIN_ID = "x-admin-id";

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final TypeRef<Map<CreationEnum, List<?>>> createdResponseType = new TypeRef<>() {};
    private static final TypeRef<Map<String, Optional<String>>> emailsResponseType = new TypeRef<>() {};

    private Map<String, String> bearer;
    private PiUser systemAdminUser;
    private String ctscSuperAdminId;
    private String localSuperAdminId;
    private String localAdminId;
    private String thirdPartyUserId;

    @BeforeAll
    public void startUp() throws JsonProcessingException {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        String email = generateEmail();

        SystemAdminAccount systemAdminAccount = new SystemAdminAccount();
        systemAdminAccount.setEmail(email);
        systemAdminAccount.setSurname("AM E2E Surname");
        systemAdminAccount.setFirstName("AM E2E First Name");
        systemAdminAccount.setProvenanceUserId(UUID.randomUUID().toString());

        systemAdminUser = doPostRequest(CREATE_SYSTEM_ADMIN_SSO,
                      bearer, objectMapper.writeValueAsString(systemAdminAccount)).getBody().as(PiUser.class);


        ctscSuperAdminId = (String) createAccount(generateEmail(), UUID.randomUUID().toString(), Roles.INTERNAL_SUPER_ADMIN_CTSC)
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        localSuperAdminId = (String) createAccount(generateEmail(), UUID.randomUUID().toString(), Roles.INTERNAL_SUPER_ADMIN_LOCAL)
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        localAdminId = (String) createAccount(generateEmail(), UUID.randomUUID().toString(), Roles.INTERNAL_ADMIN_LOCAL)
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, bearer);
        doDeleteRequest(String.format(DELETE_ENDPOINT, thirdPartyUserId), bearer);
    }

    private String generateEmail() {
        return TEST_EMAIL_PREFIX + "-" +
            ThreadLocalRandom.current().nextInt(1000, 9999) + "@justice.gov.uk";
    }

    private Response createAccount(String email, String provenanceId) throws JsonProcessingException {
        return createAccount(email, provenanceId, Roles.VERIFIED, UserProvenances.PI_AAD);
    }

    private Response createAccount(String email, String provenanceId, Roles role) throws JsonProcessingException {
        return createAccount(email, provenanceId, role, UserProvenances.PI_AAD);
    }

    private Response createAccount(String email, String provenanceId, Roles role, UserProvenances userProvenance) throws JsonProcessingException {
        PiUser piUser = new PiUser();
        piUser.setEmail(email);
        piUser.setRoles(role);
        piUser.setForenames("TEST");
        piUser.setSurname("USER");
        piUser.setUserProvenance(userProvenance);
        piUser.setProvenanceUserId(provenanceId);

        List<PiUser> users = new ArrayList<>();
        users.add(piUser);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ISSUER_ID, UUID.randomUUID().toString());

        final Response createResponse = doPostRequest(CREATE_PI_ACCOUNT_PATH,
                                                      headers, objectMapper.writeValueAsString(users));

        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED.value());

        return createResponse;
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
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);
        assertThat(createdResponse.getBody().as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).size())
            .isEqualTo(1);
    }

    @Test
    void shouldNotBeAbleToCreateThirdPartyAccountIfNotSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ISSUER_ID, ctscSuperAdminId);

        final Response createResponse = doPostRequest(CREATE_PI_ACCOUNT_PATH,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());
    }

    @Test
    void shouldBeAbleToCreateThirdPartyAccountIfSystemAdmin() throws Exception {
        List<PiUser> thirdParty = generateThirdParty();

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminUser.getUserId());

        final Response createdResponse = doPostRequest(CREATE_PI_ACCOUNT_PATH,
                                                      headers, objectMapper.writeValueAsString(thirdParty));

        assertThat(createdResponse.getStatusCode()).isEqualTo(CREATED.value());

        thirdPartyUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);
    }

    @Test
    void shouldBeAbleToGetAnAccountByUserId() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response getUserResponse = doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(PiUser.class)).matches(user -> user.getEmail().equals(email));
    }

    @Test
    void shouldBeAbleToGetAnAccountByProvenanceId() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        createAccount(email, provenanceId);

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(PiUser.class)).matches(user -> user.getEmail().equals(email));
    }

    @Test
    void checkIfUserIsAuthorisedWhenPublicList() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PUBLIC
        ), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isTrue();
    }

    @Test
    void checkIfUserIsAuthorisedWhenPrivateList() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.PRIVATE
        ), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isTrue();
    }

    @Test
    void checkIfUserIsAuthorisedWhenClassifiedList() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.SJP_PUBLIC_LIST, Sensitivity.CLASSIFIED
        ), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isTrue();
    }

    @Test
    void checkIfUserIsNotAuthorisedWhenClassifiedList() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response getUserResponse = doGetRequest(String.format(USER_IS_AUTHORISED_FOR_LIST, createdUserId,
                                                              ListType.CIVIL_DAILY_CAUSE_LIST, Sensitivity.CLASSIFIED
        ), bearer);

        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getUserResponse.getBody().as(Boolean.class)).isFalse();
    }

    @Test
    void shouldBeAbleToMapUserEmails() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        String unknownUserId = UUID.randomUUID().toString();

        Response getEmailsResponse = doPostRequest(EMAILS_ENDPOINT, bearer,
                                                 objectMapper.writeValueAsString(
                                                     List.of(createdUserId, unknownUserId)));

        assertThat(getEmailsResponse.getStatusCode()).isEqualTo(OK.value());

        Map<String, Optional<String>> responseBody = getEmailsResponse.getBody().as(emailsResponseType);

        assertThat(responseBody).matches(map -> map.containsKey(createdUserId));
        assertThat(responseBody).matches(map -> map.containsKey(unknownUserId));

        assertThat(responseBody.get(createdUserId)).isPresent().matches(returnedEmail ->
                                                                            returnedEmail.get().equals(email));
        assertThat(responseBody.get(unknownUserId)).isNotPresent();
    }

    @Test
    void shouldBeAbleToUpdateAccount() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        createAccount(email, provenanceId);

        Map<String, String> updateParams = new HashMap<>();
        updateParams.put("lastVerifiedDate", "2024-12-01T01:01:01.123456Z");
        updateParams.put("lastSignedInDate", "2024-12-02T01:01:01.123456Z");

        Response updateResponse = doPutRequestWithBody(String.format(UPDATE_ACCOUNT_ENDPOINT, provenanceId), bearer,
                             objectMapper.writeValueAsString(updateParams));

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        PiUser user = getUserResponse.getBody().as(PiUser.class);

        assertThat(user.getLastVerifiedDate()).isEqualTo(LocalDateTime.parse("2024-12-01T01:01:01"));
        assertThat(user.getLastSignedInDate()).isEqualTo(LocalDateTime.parse("2024-12-02T01:01:01"));
    }

    @Test
    void shouldBeAbleToDeleteAccount() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT, createdUserId), bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountV2WhenSystemAdmin() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, systemAdminUser.getUserId());

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountV2WhenCTSCSuperAdmin() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, ctscSuperAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountV2WhenLocalSuperAdmin() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, ctscSuperAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldNotBeAbleToDeleteAccountWhenLocalAdmin() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, localAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldBeAbleToDeleteAccountWhenSSO() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.SSO);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, localAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(OK.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldNotBeAbleToDeleteVerifiedAccountV2WhenNonSystemAdmin() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, ctscSuperAdminId);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), headers);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToDeleteAccountWhenUserNotProvided() throws Exception {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Response deleteResponse = doDeleteRequest(String.format(DELETE_ENDPOINT_V2, createdUserId), bearer);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        Response getUserResponse = doGetRequest(String.format(GET_BY_PROVENANCE_ID, provenanceId), bearer);
        assertThat(getUserResponse.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldNotBeAbleToUpdateTheirOwnAccount() {
        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, ctscSuperAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, ctscSuperAdminId, "INTERNAL_ADMIN_LOCAL"), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, ctscSuperAdminId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleWhenUserNotProvided() {
        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, ctscSuperAdminId, "INTERNAL_ADMIN_LOCAL"), bearer);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, ctscSuperAdminId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldBeAbleToUpdateRoleIfSystemAdmin() throws JsonProcessingException {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, systemAdminUser.getUserId());

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, createdUserId, "INTERNAL_SUPER_ADMIN_CTSC"), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldBeAbleToUpdateRoleIfCTSCSuperAdmin() throws JsonProcessingException {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, ctscSuperAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, createdUserId, "INTERNAL_SUPER_ADMIN_CTSC"), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldBeAbleToUpdateRoleIfCTSCLocalAdmin() throws JsonProcessingException {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, localSuperAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, createdUserId, "INTERNAL_SUPER_ADMIN_CTSC"), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleIfNotSuperAdmin() throws JsonProcessingException {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, localAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, createdUserId, "INTERNAL_SUPER_ADMIN_CTSC"), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_ADMIN_LOCAL);
    }

    @Test
    void shouldNotBeAbleToUpdateRoleIfVerifiedAccount() throws JsonProcessingException {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, localSuperAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, createdUserId, "INTERNAL_SUPER_ADMIN_CTSC"), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(FORBIDDEN.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.VERIFIED);
    }

    @Test
    void shouldBeAbleToUpdateRoleIfSSO() throws JsonProcessingException {
        String email = generateEmail();
        String provenanceId = UUID.randomUUID().toString();
        Response createdResponse = createAccount(email, provenanceId, Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.SSO);

        String createdUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ADMIN_ID, localAdminId);

        Response updateResponse = doPutRequest(
            String.format(UPDATE_ACCOUNT_ROLE_ENDPOINT, createdUserId, "INTERNAL_SUPER_ADMIN_CTSC"), headers);

        assertThat(updateResponse.getStatusCode()).isEqualTo(OK.value());

        PiUser getUserResponse =
            doGetRequest(String.format(GET_BY_USER_ID, createdUserId), bearer).getBody().as(PiUser.class);

        assertThat(getUserResponse.getRoles()).isEqualTo(Roles.INTERNAL_SUPER_ADMIN_CTSC);
    }

}
