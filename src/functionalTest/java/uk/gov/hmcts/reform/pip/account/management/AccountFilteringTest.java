package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.account.management.utils.CustomPageImpl;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.OK;

@SuppressWarnings("PMD.TooManyMethods")
class AccountFilteringTest extends AccountHelperBase {
    private static final TypeRef<CustomPageImpl<PiUser>> GET_ALL_USERS_TYPE = new TypeRef<>() {};

    private PiUser systemAdminUser;
    private String thirdPartyUserId;
    private Map<String, String> headers;

    private static final String GET_ALL_THIRD_PARTY_ACCOUNTS = "/account/all/third-party";
    private static final String DELETE_ACCOUNT = "/account/delete/%s";
    private static final String GET_ADMIN_USER_BY_EMAIL_AND_PROVENANCE = "/account/admin/%s/%s";
    private static final String GET_ACCOUNTS_EXCEPT_THIRD_PARTY = "/account/all";
    private static final String MI_DATA_URL = "/account/mi-data";

    @BeforeAll
    public void startUp() throws JsonProcessingException {
        systemAdminUser = createSystemAdminAccount();

        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.GENERAL_THIRD_PARTY);
        piUser.setUserProvenance(UserProvenances.THIRD_PARTY);
        piUser.setProvenanceUserId(generateEmail());

        List<PiUser> thirdPartyList = new ArrayList<>();
        thirdPartyList.add(piUser);

        headers = addAuthHeader();

        Response createdResponse =
            doPostRequest(CREATE_PI_ACCOUNT, headers, objectMapper.writeValueAsString(thirdPartyList));

        thirdPartyUserId = getCreatedAccountUserId(createdResponse);
    }

    private Map<String, String> addAuthHeader() {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);

        headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminUser.getUserId());

        return headers;
    }

    private void verifyFilteringController(Map<String, String> requestParams) {
        Response response = doGetRequestWithRequestParams(GET_ACCOUNTS_EXCEPT_THIRD_PARTY, headers, requestParams);
        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        Page<PiUser> returnedPage = response.getBody().as(GET_ALL_USERS_TYPE);
        assertThat(returnedPage.getTotalElements()).isEqualTo(1);
        assertThat(returnedPage.getContent().getFirst())
            .matches(user -> user.getUserId().equals(systemAdminUser.getUserId()));
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, headers);
        doDeleteRequest(String.format(DELETE_ACCOUNT, thirdPartyUserId), headers);
    }

    @Test
    void testGetAllThirdPartyAccounts() {
        Response getThirdParties = doGetRequest(GET_ALL_THIRD_PARTY_ACCOUNTS, headers);

        assertThat(getThirdParties.getStatusCode()).isEqualTo(OK.value());

        PiUser[] thirdParties = getThirdParties.getBody().as(PiUser[].class);

        assertThat(thirdParties)
            .anyMatch(thirdParty -> thirdParty.getUserId().equals(thirdPartyUserId));
    }

    @Test
    void testGetAllAccountsExceptThirdPartyDoesNotContainThirdParty() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("provenances", UserProvenances.THIRD_PARTY.toString());

        Response response = doGetRequestWithRequestParams(GET_ACCOUNTS_EXCEPT_THIRD_PARTY, headers, requestParams);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        Page<PiUser> returnedPage = response.getBody().as(GET_ALL_USERS_TYPE);
        assertThat(returnedPage.getTotalElements()).isEqualTo(0);
    }

    @Test
    void testGetAllAccountsExceptThirdPartyContainsAdminUser() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("userId", systemAdminUser.getUserId());
        verifyFilteringController(requestParams);
    }

    @Test
    void testGetAllAccountsExceptThirdPartyWithProvenanceId() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("userProvenanceId", systemAdminUser.getProvenanceUserId());
        verifyFilteringController(requestParams);
    }

    @Test
    void testGetAllAccountsExceptThirdPartyWithRole() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("userProvenanceId", systemAdminUser.getProvenanceUserId());
        requestParams.put("roles", "SYSTEM_ADMIN");
        verifyFilteringController(requestParams);
    }

    @Test
    void testGetAllAccountsExceptThirdPartyWithEmail() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("email", systemAdminUser.getEmail());
        verifyFilteringController(requestParams);
    }

    @Test
    void testGetAllAccountsExceptThirdPartyWithPageSizeAndPageNumber() {
        Map<String, String> requestParams = new ConcurrentHashMap<>();
        requestParams.put("pageSize", "1");
        requestParams.put("pageNumber", "1");

        Response response = doGetRequestWithRequestParams(GET_ACCOUNTS_EXCEPT_THIRD_PARTY, headers, requestParams);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        Page<PiUser> returnedPage = response.getBody().as(GET_ALL_USERS_TYPE);
        assertThat(returnedPage.getContent().size()).isEqualTo(1);
    }

    @Test
    void testGetAdminUserByEmailAndProvenance() throws JsonProcessingException {
        String email = generateEmail();
        createAccount(email, UUID.randomUUID().toString(), Roles.INTERNAL_ADMIN_LOCAL, UserProvenances.PI_AAD,
                      systemAdminUser.getUserId());

        Response response = doGetRequest(String.format(GET_ADMIN_USER_BY_EMAIL_AND_PROVENANCE, email,
                                                       UserProvenances.PI_AAD
        ), bearer);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        PiUser adminUser = response.getBody().as(PiUser.class);
        assertThat(adminUser.getEmail()).isEqualTo(email);
    }

    @Test
    void testGetMiData() {
        Response response = doGetRequest(MI_DATA_URL, bearer);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        List<AccountMiData> returnedAccounts = Arrays.asList(response.getBody()
                                                                 .as(AccountMiData[].class));

        Assertions.assertThat(returnedAccounts).anyMatch(
            account -> systemAdminUser.getUserId().equals(account.getUserId().toString())
        );
    }
}
