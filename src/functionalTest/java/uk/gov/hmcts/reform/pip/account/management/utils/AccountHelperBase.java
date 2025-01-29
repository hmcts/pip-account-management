package uk.gov.hmcts.reform.pip.account.management.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.CREATED;

public class AccountHelperBase extends FunctionalTestBase {

    //Header Values
    protected static final String BEARER = "Bearer ";
    protected static final String ISSUER_ID = "x-issuer-id";

    //Utils
    protected static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    protected ObjectMapper objectMapper = new ObjectMapper();
    protected static final TypeRef<Map<CreationEnum, List<?>>> CREATED_RESPONSE_TYPE = new TypeRef<>() {};

    //Endpoints
    protected static final String TESTING_SUPPORT_DELETE_ACCOUNT_URL = "/testing-support/account/";
    protected static final String CREATE_PI_ACCOUNT = "/account/add/pi";
    private static final String CREATE_SYSTEM_ADMIN_SSO = "/account/system-admin";

    protected Map<String, String> bearer;

    protected String generateEmail() {
        return TEST_EMAIL_PREFIX + "-"
            + ThreadLocalRandom.current().nextInt(1000, 9999) + "@justice.gov.uk";
    }

    protected PiUser createSystemAdminAccount() throws JsonProcessingException {
        SystemAdminAccount systemAdminAccount = new SystemAdminAccount();
        systemAdminAccount.setEmail(generateEmail());
        systemAdminAccount.setSurname("AM E2E Surname");
        systemAdminAccount.setFirstName("AM E2E First Name");
        systemAdminAccount.setProvenanceUserId(UUID.randomUUID().toString());

        return doPostRequest(CREATE_SYSTEM_ADMIN_SSO,
                             bearer, objectMapper.writeValueAsString(systemAdminAccount)).getBody().as(PiUser.class);
    }

    protected Response createAccount(String email, String provenanceId) throws JsonProcessingException {
        return createAccount(email, provenanceId, Roles.VERIFIED, UserProvenances.PI_AAD);
    }

    protected Response createAccount(String email, String provenanceId, Roles role) throws JsonProcessingException {
        return createAccount(email, provenanceId, role, UserProvenances.PI_AAD);
    }

    protected Response createAccount(String email, String provenanceId, Roles role, UserProvenances userProvenance)
        throws JsonProcessingException {
        PiUser piUser = new PiUser();
        piUser.setEmail(email);
        piUser.setRoles(role);
        piUser.setForenames("TEST");
        piUser.setSurname("USER");
        piUser.setUserProvenance(userProvenance);
        piUser.setProvenanceUserId(provenanceId);

        List<PiUser> users = new ArrayList<>();
        users.add(piUser);

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, UUID.randomUUID().toString());

        final Response createResponse = doPostRequest(CREATE_PI_ACCOUNT,
                                                      headers, objectMapper.writeValueAsString(users));

        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED.value());

        return createResponse;
    }

    protected String getCreatedAccountUserId(Response response) {
        return (String) response.getBody().as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).get(0);
    }
}
