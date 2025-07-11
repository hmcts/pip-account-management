package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.account.management.model.account.AuditLog;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

class AuditCreationTest extends AccountHelperBase {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TEST_EMAIL_PREFIX = String.format(
        "pip-am-test-email-%s", ThreadLocalRandom.current().nextInt(1000, 9999));
    private static final String TEST_EMAIL = TEST_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String ROLES = "SYSTEM_ADMIN";
    private static final String USER_PROVENANCE = "PI_AAD";
    private static final String ACTION = "PUBLICATION_UPLOAD";
    private static final String DETAILS = "Publication with artefact id %s successfully uploaded";
    private static final Integer PAGE_NUMBER = 0;
    private static final Integer PAGE_SIZE = 2;

    private static final String AUDIT_URL = "/audit";
    private static final String GET_AUDIT_URL = "/audit/%s";
    private static final String TESTING_SUPPORT_AUDIT_URL = "/testing-support/audit/";
    private static final String CONTENT = "content";


    private Map<String, String> headers;

    @BeforeAll
    public void startUp() throws JsonProcessingException {
        bearer = Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);

        PiUser systemAdminUser;
        systemAdminUser = createSystemAdminAccount();

        headers = new ConcurrentHashMap<>(bearer);
        headers.put(ISSUER_ID, systemAdminUser.getUserId());
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_AUDIT_URL + TEST_EMAIL_PREFIX, headers);
    }

    private AuditLog createAuditLog() {

        UUID artefactId = UUID.randomUUID();
        String responseBody = """
            {
                "userId": "%s",
                "userEmail": "%s",
                "roles": "%s",
                "userProvenance": "%s",
                "action": "%s",
                "details": "%s"
            }
            """.formatted(USER_ID, TEST_EMAIL, ROLES, USER_PROVENANCE, ACTION, String.format(DETAILS, artefactId));

        Response response = doPostRequest(AUDIT_URL, bearer, responseBody);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());

        return response.getBody().as(AuditLog.class);
    }

    private void deleteAuditLog() {
        Response response = doDeleteRequest(AUDIT_URL, bearer);

        assertThat(response.getStatusCode()).isEqualTo(OK.value());
    }

    @Test
    void shouldBeAbleToCreateAndGetAnAuditRecord() {
        AuditLog auditLog = createAuditLog();

        Response getResponse = doGetRequest(String.format(GET_AUDIT_URL, auditLog.getId()), headers);
        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());

        AuditLog retrievedAuditLog = getResponse.getBody().as(AuditLog.class);
        assertThat(retrievedAuditLog.getId()).isEqualTo(auditLog.getId());
        assertThat(retrievedAuditLog.getUserEmail()).isEqualTo(auditLog.getUserEmail());
    }

    @Test
    void shouldBeAbleToGetAllAuditLogsUsingDefaultDisplayParameters() {
        AuditLog auditLog = createAuditLog();

        Response getResponse = doGetRequestWithQueryParameters(String.format(AUDIT_URL), headers,
                                                               "", "", TEST_EMAIL_PREFIX);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getResponse.jsonPath().getInt("pageable.pageNumber")).isEqualTo(0);
        assertThat(getResponse.jsonPath().getInt("pageable.pageSize")).isEqualTo(25);

        List<AuditLog> retrievedAuditLogs = getResponse.jsonPath().getList(CONTENT, AuditLog.class);

        assertThat(retrievedAuditLogs).isNotNull();
        assertThat(retrievedAuditLogs.size()).isEqualTo(2);
        assertThat(retrievedAuditLogs.getFirst().getId()).isEqualTo(auditLog.getId());
    }

    @Test
    void shouldBeAbleToGetAllAuditLogsUsingCustomDisplayParameters() {
        AuditLog auditLog = createAuditLog();

        Response getResponse = doGetRequestWithQueryParameters(String.format(AUDIT_URL), headers,
                                                   PAGE_NUMBER.toString(), PAGE_SIZE.toString(), TEST_EMAIL_PREFIX);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK.value());
        assertThat(getResponse.jsonPath().getInt("pageable.pageNumber")).isEqualTo(0);
        assertThat(getResponse.jsonPath().getInt("pageable.pageSize")).isEqualTo(2);
        assertThat(getResponse.jsonPath().getInt("totalPages")).isEqualTo(2);
        assertThat(getResponse.jsonPath().getInt("totalElements")).isEqualTo(4);

        List<AuditLog> retrievedAuditLogs = getResponse.jsonPath().getList(CONTENT, AuditLog.class);

        assertThat(retrievedAuditLogs).isNotNull();
        assertThat(retrievedAuditLogs.getFirst().getId()).isEqualTo(auditLog.getId());
        assertThat(retrievedAuditLogs.size()).isEqualTo(2);
    }

    @Test
    void shouldReturnErrorWhenAuditRecordDoesNotExist() {
        Response getResponse = doGetRequest(String.format(GET_AUDIT_URL, USER_ID), headers);

        assertThat(getResponse.getStatusCode()).isEqualTo(NOT_FOUND.value());
    }

    @Test
    void shouldReturnOkForDeleteAuditLogsIfAllInDate() {
        AuditLog auditLog = createAuditLog();

        deleteAuditLog();

        Response getResponse = doGetRequestWithQueryParameters(String.format(AUDIT_URL), headers,
                                                               "", "", TEST_EMAIL_PREFIX);

        List<AuditLog> retrievedAuditLogs = getResponse.jsonPath().getList(CONTENT, AuditLog.class);

        assertThat(retrievedAuditLogs).isNotNull();
        assertThat(retrievedAuditLogs.getFirst().getId()).isEqualTo(auditLog.getId());
        assertThat(retrievedAuditLogs.size()).isEqualTo(3);
    }

    @Test
    void shouldBeAbleToDeleteAnOutOfDateAudit() {
        Response getResponse = doGetRequestWithQueryParameters(String.format(AUDIT_URL), headers,
                                                               "", "", TEST_EMAIL_PREFIX);
        List<AuditLog> retrievedAuditLogs = getResponse.jsonPath().getList(CONTENT, AuditLog.class);
        assertThat(retrievedAuditLogs.size()).isEqualTo(4);

        AuditLog auditLog = createAuditLog();
        doPutRequest(TESTING_SUPPORT_AUDIT_URL + auditLog.getId(), bearer);

        getResponse = doGetRequestWithQueryParameters(String.format(AUDIT_URL), headers, "", "", TEST_EMAIL_PREFIX);
        retrievedAuditLogs = getResponse.jsonPath().getList(CONTENT, AuditLog.class);
        assertThat(retrievedAuditLogs.size()).isEqualTo(5);

        deleteAuditLog();

        getResponse = doGetRequestWithQueryParameters(String.format(AUDIT_URL), headers, "", "", TEST_EMAIL_PREFIX);
        retrievedAuditLogs = getResponse.jsonPath().getList(CONTENT, AuditLog.class);
        assertThat(retrievedAuditLogs.size()).isEqualTo(4);
    }

}
