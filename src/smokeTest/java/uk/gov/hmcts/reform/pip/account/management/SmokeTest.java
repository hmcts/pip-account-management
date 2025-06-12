package uk.gov.hmcts.reform.pip.account.management;

import io.restassured.common.mapper.TypeRef;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;
import uk.gov.hmcts.reform.pip.account.management.model.account.AzureAccount;
import uk.gov.hmcts.reform.pip.account.management.model.account.CreationEnum;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.utils.OAuthClient;
import uk.gov.hmcts.reform.pip.account.management.utils.SmokeTestBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;

@SpringBootTest(classes = {OAuthClient.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("smoke")
class SmokeTest extends SmokeTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String BASE_ACCOUNT_URL = "/account";
    private static final String CREATE_PI_ACCOUNT_URL = BASE_ACCOUNT_URL + "/add/pi";
    private static final String CREATE_AZURE_ACCOUNT_URL = BASE_ACCOUNT_URL + "/add/azure";
    private static final String MEDIA_APPLICATION_URL = "/application";
    private static final String SUBSCRIPTION_URL = "/subscription";

    private static final String TESTING_SUPPORT_DELETE_ACCOUNT_URL = "/testing-support/account/";
    private static final String TESTING_SUPPORT_APPLICATION_URL = "/testing-support/application/";
    private static final String TESTING_SUPPORT_SUBSCRIPTION_URL = "/testing-support/subscription/";

    private static final String ISSUER_ID_HEADER = "x-issuer-id";
    private static final String USER_ID_HEADER = "x-user-id";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String TEST_FIRST_NAME = "SmokeTestFirstName";
    private static final String TEST_SURNAME = "SmokeTestSurname";
    private static final String TEST_DISPLAY_NAME = "SmokeTestName";
    private static final String TEST_EMPLOYER = "SmokeTestEmployer";
    private static final String TEST_EMAIL_PREFIX = "SmokeTestEmail-"
        + ThreadLocalRandom.current().nextInt(1000, 9999);
    private static final String TEST_EMAIL = TEST_EMAIL_PREFIX + "@justice.gov.uk";
    private static final String LOCATION_ID = createRandomId();
    private static final String LOCATION_NAME = "TestLocation" + LOCATION_ID;
    protected static final TypeRef<Map<CreationEnum, List<?>>> CREATED_RESPONSE_TYPE = new TypeRef<>() {};

    private static final String MOCK_FILE = "data/test-image.png";
    private static final TypeRef<Map<CreationEnum, List<? extends AzureAccount>>> AZURE_ACCOUNT_RESPONSE_TYPE
        = new TypeRef<>() {};

    private static final String STATUS_CODE_MATCH = "Status code does not match";
    private static final String RESPONSE_BODY_MATCH = "Response body does not match";
    private static String verifiedUserId;

    @Value("${test-system-admin-id}")
    private String systemAdminUserId;

    @BeforeAll
    public void setup() throws JsonProcessingException {
        OBJECT_MAPPER.findAndRegisterModules();
        createTestLocation(LOCATION_ID, LOCATION_NAME);

        PiUser piUser = new PiUser();
        piUser.setEmail(TEST_EMAIL_PREFIX + "-"
                            + ThreadLocalRandom.current().nextInt(1000, 9999) + "@justice.gov.uk");
        piUser.setRoles(Roles.VERIFIED);
        piUser.setForenames("SmokeTestSubscription-Firstname");
        piUser.setSurname("SmokeTestSubscription-Surname");
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setProvenanceUserId(UUID.randomUUID().toString());

        verifiedUserId = (String)
            doPostRequest(CREATE_PI_ACCOUNT_URL, Map.of(ISSUER_ID_HEADER, systemAdminUserId),
                          OBJECT_MAPPER.writeValueAsString(List.of(piUser)))
                .getBody()
                .as(CREATED_RESPONSE_TYPE)
                .get(CreationEnum.CREATED_ACCOUNTS)
                .getFirst();
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX);
        doDeleteRequest(TESTING_SUPPORT_APPLICATION_URL + TEST_EMAIL_PREFIX);
        doDeleteRequest(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME);
        deleteTestLocation(LOCATION_NAME);
    }

    @Test
    void testHealthCheck() {
        Response response = doGetRequest("");

        assertThat(response.statusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(OK.value());

        assertThat(response.body().asString())
            .as(RESPONSE_BODY_MATCH)
            .isEqualTo("Welcome to account-management");
    }

    @Test
    void testCreateUserAccount() throws JsonProcessingException {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setFirstName(TEST_FIRST_NAME);
        azureAccount.setSurname(TEST_SURNAME);
        azureAccount.setDisplayName(TEST_DISPLAY_NAME);
        azureAccount.setRole(Roles.VERIFIED);
        azureAccount.setEmail(TEST_EMAIL);

        Response response = doPostRequest(CREATE_AZURE_ACCOUNT_URL, Map.of(ISSUER_ID_HEADER, systemAdminUserId),
                                          OBJECT_MAPPER.writeValueAsString(List.of(azureAccount)));

        String azureAccountId = response.getBody().as(AZURE_ACCOUNT_RESPONSE_TYPE)
            .get(CreationEnum.CREATED_ACCOUNTS)
            .getFirst()
            .getAzureAccountId();

        PiUser piUser = new PiUser();
        piUser.setEmail(TEST_EMAIL);
        piUser.setRoles(Roles.VERIFIED);
        piUser.setForenames(TEST_FIRST_NAME);
        piUser.setSurname(TEST_SURNAME);
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setProvenanceUserId(azureAccountId);

        response = doPostRequest(CREATE_PI_ACCOUNT_URL, Map.of(ISSUER_ID_HEADER, systemAdminUserId),
                                 OBJECT_MAPPER.writeValueAsString(List.of(piUser)));

        assertThat(response.getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(CREATED.value());
    }

    @Test
    void testCreateMediaApplication() throws IOException {
        Response response = doPostMultipartForApplication(MEDIA_APPLICATION_URL, systemAdminUserId,
                                                          new ClassPathResource(MOCK_FILE).getFile(),
                                                          TEST_DISPLAY_NAME, TEST_EMAIL, TEST_EMPLOYER,
                                                          MediaApplicationStatus.PENDING.toString());

        assertThat(response.getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(OK.value());
    }

    @Test
    void testCreateSubscription() {
        Subscription subscription = new Subscription();
        subscription.setUserId(UUID.fromString(verifiedUserId));
        subscription.setSearchType(SearchType.LOCATION_ID);
        subscription.setSearchValue(LOCATION_ID);
        subscription.setChannel(Channel.EMAIL);
        subscription.setCreatedDate(LocalDateTime.now());
        subscription.setLocationName(LOCATION_NAME);
        subscription.setLastUpdatedDate(LocalDateTime.now());

        Response response = doPostRequest(SUBSCRIPTION_URL, Map.of(USER_ID_HEADER, USER_ID.toString()), subscription);

        assertThat(response.getStatusCode())
            .as(STATUS_CODE_MATCH)
            .isEqualTo(CREATED.value());
    }
}
