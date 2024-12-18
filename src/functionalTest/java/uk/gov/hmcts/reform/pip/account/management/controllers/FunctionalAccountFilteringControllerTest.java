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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.hmcts.reform.pip.account.management.model.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.account.management.utils.FunctionalTestBase;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
@ActiveProfiles(profiles = "functional")
@SpringBootTest(classes = {OAuthClient.class})
public class FunctionalAccountFilteringControllerTest extends FunctionalTestBase {

    private static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String BEARER = "Bearer ";
    private static final String ISSUER_ID = "x-issuer-id";
    private static final TypeRef<Map<CreationEnum, List<?>>> createdResponseType = new TypeRef<>() {};
    private static final TypeRef<PageImpl<PiUser>> getAllUsersType = new TypeRef<>() {};

    private Map<String, String> bearer;
    private PiUser systemAdminUser;
    private String thirdPartyUserId;

    private static final String TESTING_SUPPORT_DELETE_ACCOUNT_URL = "/testing-support/account/";
    private static final String GET_ALL_THIRD_PARTY_ACCOUNTS_ENDPOINT = "/account/all/third-party";
    private static final String CREATE_SYSTEM_ADMIN_SSO = "/account/system-admin";
    private static final String CREATE_PI_ACCOUNT_PATH = "/account/add/pi";
    private static final String DELETE_ENDPOINT = "/account/delete/%s";
    private static final String GET_ADMIN_USER_BY_EMAIL_AND_PROVENANCE = "/account/admin/%s/%s";
    private static final String GET_ACCOUNTS_EXCEPT_THIRD_PARTY = "/account/all";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private String generateEmail() {
        return TEST_EMAIL_PREFIX + "-" +
            ThreadLocalRandom.current().nextInt(1000, 9999) + "@justice.gov.uk";
    }

    @BeforeAll
    public void startUp() throws JsonProcessingException {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        SystemAdminAccount systemAdminAccount = new SystemAdminAccount();
        systemAdminAccount.setEmail(generateEmail());
        systemAdminAccount.setSurname("AM E2E Surname");
        systemAdminAccount.setFirstName("AM E2E First Name");
        systemAdminAccount.setProvenanceUserId(UUID.randomUUID().toString());

        systemAdminUser = doPostRequest(CREATE_SYSTEM_ADMIN_SSO,
                                        bearer, objectMapper.writeValueAsString(systemAdminAccount))
            .getBody().as(PiUser.class);

        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.GENERAL_THIRD_PARTY);
        piUser.setUserProvenance(UserProvenances.THIRD_PARTY);
        piUser.setProvenanceUserId(generateEmail());

        List<PiUser> thirdPartyList = new ArrayList<>();
        thirdPartyList.add(piUser);

        Map<String, String> headers = new HashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminUser.getUserId());

        Response createdResponse =
            doPostRequest(CREATE_PI_ACCOUNT_PATH, headers, objectMapper.writeValueAsString(thirdPartyList));

        thirdPartyUserId = (String) createdResponse.getBody()
            .as(createdResponseType).get(CreationEnum.CREATED_ACCOUNTS).get(0);
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, bearer);
        doDeleteRequest(String.format(DELETE_ENDPOINT, thirdPartyUserId), bearer);
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

    @Test
    void getAllThirdPartyAccounts() {
        Response getThirdParties = doGetRequest(GET_ALL_THIRD_PARTY_ACCOUNTS_ENDPOINT, bearer);

        assertThat(getThirdParties.getStatusCode()).isEqualTo(OK.value());

        PiUser[] thirdParties = getThirdParties.getBody().as(PiUser[].class);

        assertThat(thirdParties)
            .anyMatch(thirdParty -> thirdParty.getUserId().equals(thirdPartyUserId));
    }

    @Test
    void getAllAccountsExceptThirdPartyDoesNotContainThirdParty() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("userId", thirdPartyUserId);

        Response response = doGetRequestWithRequestParams(String.format(GET_ACCOUNTS_EXCEPT_THIRD_PARTY,
                                                       UserProvenances.PI_AAD), bearer, requestParams);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        Page<PiUser> returnedPage = response.getBody().as(getAllUsersType);
        assertThat(returnedPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    void getAllAccountsExceptThirdPartyContainsAdminUser() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("userId", systemAdminUser.getUserId());

        Response response = doGetRequestWithRequestParams(String.format(GET_ACCOUNTS_EXCEPT_THIRD_PARTY,
                                                                        UserProvenances.PI_AAD), bearer, requestParams);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        Page<PiUser> returnedPage = response.getBody().as(getAllUsersType);
        assertThat(returnedPage.getTotalElements()).isEqualTo(1);
        assertThat(returnedPage.getContent().get(0))
            .matches(user -> user.getUserId().equals(systemAdminUser.getUserId()));
    }

    @Test
    void getAdminUserByEmailAndProvenance() throws JsonProcessingException {
        String email = generateEmail();
        createAccount(email, UUID.randomUUID().toString(), Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.PI_AAD);

        Response response = doGetRequest(String.format(GET_ADMIN_USER_BY_EMAIL_AND_PROVENANCE, email,
                                   UserProvenances.PI_AAD), bearer);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        PiUser adminUser = response.getBody().as(PiUser.class);
        assertThat(adminUser.getEmail()).isEqualTo(email);
    }

}
