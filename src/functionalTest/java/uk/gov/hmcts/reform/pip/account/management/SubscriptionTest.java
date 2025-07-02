package uk.gov.hmcts.reform.pip.account.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.response.Response;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription.UserSubscription;
import uk.gov.hmcts.reform.pip.account.management.utils.AccountHelperBase;
import uk.gov.hmcts.reform.pip.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.report.AllSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.report.LocationSubscriptionMiData;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.dockerjava.zerodep.shaded.org.apache.hc.core5.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.hmcts.reform.pip.account.management.utils.TestUtil.randomLocationId;

class SubscriptionTest extends AccountHelperBase {

    @Value("${system-admin-provenance-id}")
    private String systemAdminProvenanceId;

    private static final String SUBSCRIPTION_URL = "/subscription";
    private static final String FIND_SUBSCRIPTION_BY_USER_ID_URL = "/subscription/user/";
    private static final String BUILD_SUBSCRIBER_LIST_URL = SUBSCRIPTION_URL + "/artefact-recipients";
    private static final String BUILD_DELETED_ARTEFACT_SUBSCRIBER_URL = SUBSCRIPTION_URL + "/deleted-artefact";
    private static final String CONFIGURE_LIST_TYPE_URL = SUBSCRIPTION_URL + "/configure-list-types/";
    private static final String ADD_LIST_TYPE_URL = SUBSCRIPTION_URL + "/add-list-types/";
    private static final String SUBSCRIPTION_BY_LOCATION_URL = SUBSCRIPTION_URL + "/location/";
    private static final String MI_DATA_URL = SUBSCRIPTION_URL + "/mi-data-all";
    private static final String MI_DATA_LOCATION_URL = SUBSCRIPTION_URL + "/mi-data-location";

    private static final String TESTING_SUPPORT_SUBSCRIPTION_URL = "/testing-support/subscription/";
    private static final String TESTING_SUPPORT_LOCATION_URL = "/testing-support/location/";

    private static final String LOCATION_ID = randomLocationId();
    private static final String LOCATION_NAME = "TestLocation" + LOCATION_ID;
    private static final String USER_ID_HEADER = "x-user-id";
    private static final String BEARER = "Bearer ";
    private static final String LIST_LANGUAGE = "ENGLISH";
    private static final String LIST_LANGUAGE_CONFIGURE = "WELSH";
    private static final ListType LIST_TYPE = ListType.CIVIL_DAILY_CAUSE_LIST;

    private UUID verifiedUserId = UUID.randomUUID();
    private String systemAdminUserId;

    @BeforeAll
    public void setup() throws JsonProcessingException {
        bearer = Map.of(AUTHORIZATION, BEARER + accessToken);

        PiUser systemAdminAccount = createSystemAdminAccount();
        systemAdminUserId = systemAdminAccount.getUserId();

        String createdUserId = getCreatedAccountUserId(
            createAccount(generateEmail(), UUID.randomUUID().toString(), systemAdminUserId)
        );

        verifiedUserId = UUID.fromString(createdUserId);

        doDataManagementPostRequest(
            TESTING_SUPPORT_LOCATION_URL + LOCATION_ID,
            Map.of(AUTHORIZATION, BEARER + dataManagementAccessToken), LOCATION_NAME
        );
    }

    @AfterAll
    public void teardown() {
        doDeleteRequest(TESTING_SUPPORT_DELETE_ACCOUNT_URL + TEST_EMAIL_PREFIX, bearer);
        doDeleteRequest(TESTING_SUPPORT_SUBSCRIPTION_URL + LOCATION_NAME, bearer);
        doDataManagementDeleteRequest(
            TESTING_SUPPORT_LOCATION_URL + LOCATION_NAME,
            Map.of(AUTHORIZATION, BEARER + dataManagementAccessToken)
        );
    }

    @Test
    void testGetSubscription() {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(bearer);
        headerMap.put(USER_ID_HEADER, systemAdminUserId);

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(verifiedUserId)
        );
        assertThat(responseCreateSubscription.getStatusCode()).isEqualTo(CREATED.value());
        assertThat(responseCreateSubscription.asString().contains(verifiedUserId.toString()));

        String subscriptionId = responseCreateSubscription.asString().split(" ")[5];
        Response responseFindBySubId = doGetRequest(SUBSCRIPTION_URL + "/" + subscriptionId, bearer);
        assertThat(responseFindBySubId.getStatusCode()).isEqualTo(OK.value());
        Subscription returnedSubscriptionBySubId = responseFindBySubId.as(Subscription.class);
        assertThat(returnedSubscriptionBySubId.getUserId()).isEqualTo(verifiedUserId);
        assertThat(returnedSubscriptionBySubId.getLocationName()).isEqualTo(LOCATION_NAME);

        Response responseFindByUserId = doGetRequest(FIND_SUBSCRIPTION_BY_USER_ID_URL + verifiedUserId, bearer);
        assertThat(responseFindByUserId.getStatusCode()).isEqualTo(OK.value());
        UserSubscription returnedSubscriptionByUserId = responseFindByUserId.as(UserSubscription.class);
        assertThat(returnedSubscriptionByUserId.getCaseSubscriptions().size()).isEqualTo(0);
        assertThat(returnedSubscriptionByUserId.getLocationSubscriptions().getFirst().getLocationId())
            .isEqualTo(LOCATION_ID);
        assertThat(returnedSubscriptionByUserId.getLocationSubscriptions().getFirst().getLocationName()).isEqualTo(
            LOCATION_NAME);

        Response responseUserCanDeleteSubscription = doDeleteRequest(
            SUBSCRIPTION_URL + "/" + subscriptionId,
            headerMap
        );
        assertThat(responseUserCanDeleteSubscription.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseUserCanDeleteSubscription.asString()).isEqualTo("Subscription: "
                                                                               + subscriptionId + " was deleted");
    }

    @Test
    void testConfigureSubscriptionByListType() {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(bearer);
        headerMap.put(USER_ID_HEADER, systemAdminUserId);

        doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(verifiedUserId)
        );

        SubscriptionListType listType = new SubscriptionListType(
            verifiedUserId,
            List.of(LIST_TYPE.name()),
            List.of(LIST_LANGUAGE)
        );

        Response responseAddListType = doPostRequest(
            ADD_LIST_TYPE_URL + verifiedUserId,
            headerMap,
            listType
        );
        assertThat(responseAddListType.getStatusCode()).isEqualTo(CREATED.value());
        assertThat(responseAddListType.asString()).isEqualTo(
            String.format(
                "Location list Type successfully added for user %s",
                verifiedUserId
            ));

        SubscriptionListType listTypeToConfigure = new SubscriptionListType(
            verifiedUserId,
            List.of(LIST_TYPE.name(), ListType.CIVIL_DAILY_CAUSE_LIST.name()),
            List.of(LIST_LANGUAGE_CONFIGURE)
        );

        Response responseConfigureListType = doPutRequestWithBody(
            CONFIGURE_LIST_TYPE_URL + verifiedUserId,
            headerMap,
            listTypeToConfigure
        );
        assertThat(responseConfigureListType.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseConfigureListType.asString()).isEqualTo(
            String.format(
                "Location list Type successfully updated for user %s",
                verifiedUserId
            ));
    }

    @Test
    void testGetSubscriptionByLocation() {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(bearer);
        headerMap.put(USER_ID_HEADER, systemAdminUserId);

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(verifiedUserId)
        );
        assertThat(responseCreateSubscription.getStatusCode()).isEqualTo(CREATED.value());

        final String subscriptionId = responseCreateSubscription.asString().split(" ")[5];
        Response responseFindByLocationId = doGetRequest(SUBSCRIPTION_BY_LOCATION_URL + LOCATION_ID, bearer);
        assertThat(responseFindByLocationId.getStatusCode()).isEqualTo(OK.value());
        List<Subscription> returnedSubscriptionsByLocationId = List.of(responseFindByLocationId.getBody().as(
            Subscription[].class));
        assertThat(returnedSubscriptionsByLocationId.getFirst().getUserId()).isEqualTo(verifiedUserId);
        assertThat(returnedSubscriptionsByLocationId.getFirst().getLocationName()).isEqualTo(LOCATION_NAME);

        headerMap.put("x-provenance-user-id", systemAdminProvenanceId);
        final Response responseDeleteSubscriptionByLocationId = doDeleteRequest(
            SUBSCRIPTION_BY_LOCATION_URL + LOCATION_ID,
            headerMap
        );
        assertThat(responseDeleteSubscriptionByLocationId.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseDeleteSubscriptionByLocationId.asString().contains(String.format(
            "Subscription created with the id %s for user %s",
            subscriptionId,
            verifiedUserId
        )));
    }

    @Test
    void testPostArtefactAndBulkDelete() throws Exception {

        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(bearer);
        headerMap.put(USER_ID_HEADER, systemAdminUserId);

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(verifiedUserId)
        );
        String subscriptionId = responseCreateSubscription.asString().split(" ")[5];

        try (InputStream jsonFile = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream("subscription/civilDailyCauseList.json")) {
            final String jsonString = new String(jsonFile.readAllBytes(), StandardCharsets.UTF_8);

            Response responseBuildSubscriberList = doPostRequest(
                BUILD_SUBSCRIBER_LIST_URL,
                headerMap, jsonString
            );
            assertThat(responseBuildSubscriberList.getStatusCode()).isEqualTo(ACCEPTED.value());
            assertThat(responseBuildSubscriberList.asString()).isEqualTo(
                "Subscriber request has been accepted");

            Response responseBuildDeletedArtefactSubscriberList = doPostRequest(
                BUILD_DELETED_ARTEFACT_SUBSCRIBER_URL,
                headerMap, jsonString
            );
            assertThat(responseBuildDeletedArtefactSubscriberList.getStatusCode()).isEqualTo(ACCEPTED.value());
            assertThat(responseBuildDeletedArtefactSubscriberList.asString()).isEqualTo(
                "Deleted artefact third party subscriber notification request has been accepted");
        }

        Response responseBulkDeleteSubscriptions = doDeleteRequestWithBody(
            SUBSCRIPTION_URL + "/bulk",
            headerMap, List.of(UUID.fromString(subscriptionId))
        );
        assertThat(responseBulkDeleteSubscriptions.getStatusCode()).isEqualTo(OK.value());
        assertThat(responseBulkDeleteSubscriptions.asString()).isEqualTo(
            "Subscriptions with ID " + subscriptionId + " deleted");
    }

    @Test
    void testGetMiDataAll() {
        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(bearer);
        headerMap.put(USER_ID_HEADER, verifiedUserId.toString());

        Response responseCreateSubscription = doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(verifiedUserId)
        );

        final String subscriptionId = responseCreateSubscription.asString().split(" ")[5];

        final Response responseGetSubscriptionMetadata = doGetRequest(
            MI_DATA_URL, headerMap
        );

        assertThat(responseGetSubscriptionMetadata.getStatusCode()).isEqualTo(OK.value());
        List<AllSubscriptionMiData> returnedSubscriptions = Arrays.asList(responseGetSubscriptionMetadata.getBody()
                                                                              .as(AllSubscriptionMiData[].class));

        assertThat(returnedSubscriptions).anyMatch(
            subscription -> subscriptionId.equals(subscription.getId().toString())
        );
    }

    @Test
    void testGetMiDataLocationSubscription() {
        Map<String, String> headerMap = new ConcurrentHashMap<>();
        headerMap.putAll(bearer);
        headerMap.put(USER_ID_HEADER, verifiedUserId.toString());

        doPostRequest(
            SUBSCRIPTION_URL,
            headerMap,
            createTestSubscription(verifiedUserId)
        );

        final Response responseGetLocationSubscriptionMetadata = doGetRequest(
            MI_DATA_LOCATION_URL, bearer
        );

        assertThat(responseGetLocationSubscriptionMetadata.getStatusCode())
            .isEqualTo(OK.value());
        List<LocationSubscriptionMiData> returnedLocationSubscriptions = Arrays
            .asList(responseGetLocationSubscriptionMetadata.getBody()
                        .as(LocationSubscriptionMiData[].class));

        assertThat(returnedLocationSubscriptions).anyMatch(
            locationSubscription -> LOCATION_ID.equals(locationSubscription.getSearchValue())
        );
    }

    private Subscription createTestSubscription(UUID userId) {
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setSearchType(SearchType.LOCATION_ID);
        subscription.setSearchValue(SubscriptionTest.LOCATION_ID);
        subscription.setChannel(Channel.EMAIL);
        subscription.setCreatedDate(LocalDateTime.now());
        subscription.setLocationName(SubscriptionTest.LOCATION_NAME);
        subscription.setLastUpdatedDate(LocalDateTime.now());
        return subscription;
    }
}
