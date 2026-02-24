package uk.gov.hmcts.reform.pip.account.management.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.account.SystemAdminAccount;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

public class AccountHelperBase extends FunctionalTestBase {

    //Header Values
    protected static final String REQUESTER_ID_HEADER = "x-requester-id";

    //Utils
    protected static final TypeRef<Map<CreationEnum, List<?>>> CREATED_RESPONSE_TYPE = new TypeRef<>() {};

    protected String generateEmail() {
        return TEST_EMAIL_PREFIX + "-"
            + ThreadLocalRandom.current().nextInt(1000, 9999) + "@justice.gov.uk";
    }

    protected static String generateRandomString(int length) {
        int leftLimit = 'a'; // letter 'a'
        int rightLimit = 'z'; // letter 'z'
        Random random = new Random();
        return random.ints(leftLimit, rightLimit + 1)
            .limit(length)
            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
            .toString();
    }

    protected PiUser createSystemAdminAccount() throws JsonProcessingException {
        SystemAdminAccount systemAdminAccount = new SystemAdminAccount();
        systemAdminAccount.setEmail(generateEmail());
        systemAdminAccount.setSurname("AM E2E Surname");
        systemAdminAccount.setFirstName("AM E2E First Name");
        systemAdminAccount.setProvenanceUserId(UUID.randomUUID().toString());

        final Response response = doPostRequest(CREATE_SYSTEM_ADMIN_SSO, bearer,
                                                objectMapper.writeValueAsString(systemAdminAccount));

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
        return response.getBody().as(PiUser.class);
    }

    protected Response createdVerifiedAccount(String email, String provenanceId, String requesterId)
        throws JsonProcessingException {
        return createAccount(email, provenanceId, Roles.VERIFIED, UserProvenances.PI_AAD, requesterId);
    }

    protected Response createAccount(String email, String provenanceId, Roles role, String requesterId)
        throws JsonProcessingException {
        return createAccount(email, provenanceId, role, UserProvenances.PI_AAD, requesterId);
    }

    protected Response createAccount(String email, String provenanceId, Roles role, UserProvenances userProvenance,
                                     String requesterId) throws JsonProcessingException {
        PiUser piUser = new PiUser();
        piUser.setEmail(email);
        piUser.setRoles(role);
        piUser.setForenames("TEST");
        piUser.setSurname("USER");
        piUser.setUserProvenance(userProvenance);
        piUser.setProvenanceUserId(provenanceId);

        List<PiUser> users = List.of(piUser);

        Map<String, String> headers = new ConcurrentHashMap<>(bearer);
        headers.put(REQUESTER_ID_HEADER, requesterId);

        final Response createResponse = doPostRequest(CREATE_PI_ACCOUNT, headers,
                                                      objectMapper.writeValueAsString(users));

        assertThat(createResponse.getStatusCode()).isEqualTo(CREATED.value());
        return createResponse;
    }

    protected String getCreatedAccountUserId(Response response) {
        return (String) response.getBody().as(CREATED_RESPONSE_TYPE).get(CreationEnum.CREATED_ACCOUNTS).getFirst();
    }
}
