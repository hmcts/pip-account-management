package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.Roles;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.OK;

class AzureAccountTest extends AccountHelperBase {
    private static final String CREATE_AZURE_ACCOUNT = "/account/add/azure";
    private static final String GET_AZURE_ACCOUNT_INFO = "/account/azure/%s";

    private static final String TEST_FIRST_NAME = "E2E_TEST_AM_FIRST_NAME";
    private static final String TEST_LAST_NAME = "E2E_TEST_AM_LAST_NAME";
    private static final String TEST_DISPLAY_NAME = "E2E_TEST_AM_DISPLAY_NAME";

    private static final TypeRef<Map<CreationEnum, List<? extends AzureAccount>>> AZURE_ACCOUNT_RESPONSE_TYPE
        = new TypeRef<>() {};

    @BeforeAll
    public void startUp() {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, BEARER + accessToken);
    }

    @AfterAll
    public void tearDown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, bearer);
    }

    @Test
    void testCreateAzureAccount() throws JsonProcessingException {
        String email = generateEmail();

        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setFirstName(TEST_FIRST_NAME);
        azureAccount.setSurname(TEST_LAST_NAME);
        azureAccount.setDisplayName(TEST_DISPLAY_NAME);
        azureAccount.setRole(Roles.VERIFIED);
        azureAccount.setEmail(email);

        List<AzureAccount> azureAccounts = new ArrayList<>();
        azureAccounts.add(azureAccount);

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, UUID.randomUUID().toString());

        Response response =
            doPostRequest(CREATE_AZURE_ACCOUNT, headers, objectMapper.writeValueAsString(azureAccounts));

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        AzureAccount createdAccount = response.getBody()
            .as(AZURE_ACCOUNT_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).get(0);

        assertThat(createdAccount.getEmail()).isEqualTo(email);
        createAccount(email, createdAccount.getAzureAccountId());

        Response getResponse = doGetRequest(String.format(GET_AZURE_ACCOUNT_INFO, createdAccount.getAzureAccountId()),
                     bearer);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        AzureAccount returnedUser = getResponse.getBody().as(AzureAccount.class);

        assertThat(returnedUser.getEmail()).isEqualTo(email);
    }
}
