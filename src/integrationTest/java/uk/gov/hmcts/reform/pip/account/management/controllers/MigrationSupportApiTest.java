package uk.gov.hmcts.reform.pip.account.management.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionListTypeRepository;
import uk.gov.hmcts.reform.pip.account.management.database.SubscriptionRepository;
import uk.gov.hmcts.reform.pip.account.management.database.UserRepository;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.account.UserWithSubscriptions;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;
import uk.gov.hmcts.reform.pip.account.management.utils.IntegrationTestBase;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureEmbeddedDatabase(type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES)
@WithMockUser(username = "admin", authorities = {"APPROLE_api.request.admin"})
class MigrationSupportApiTest extends IntegrationTestBase {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String ACCOUNT_URL = "/migration-support/account";
    private static final String FROM_PARAM = "lastSignedInDateFrom";
    private static final String TO_PARAM = "lastSignedInDateTo";

    private static final String UNAUTHORIZED_ROLE = "APPROLE_unknown.authorized";
    private static final String UNAUTHORIZED_USERNAME = "unauthorized_isAuthorized";

    // Dates far in the past so filtered results are not affected by users created by other tests
    private static final LocalDateTime START_OF_RANGE = LocalDateTime.of(2001, 1, 1, 0, 0, 0);
    private static final LocalDateTime END_OF_RANGE = LocalDateTime.of(2001, 1, 15, 23, 59, 59);
    private static final LocalDateTime OUTSIDE_RANGE = LocalDateTime.of(2001, 2, 1, 12, 0, 0);
    private static final LocalDateTime BEFORE_RANGE = LocalDateTime.of(2000, 6, 1, 12, 0, 0);
    private static final LocalDateTime IN_RANGE = LocalDateTime.of(2001, 1, 10, 12, 0, 0);

    private static final String LOCATION_ID = "9";
    private static final String CASE_NUMBER = "T485913";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionListTypeRepository subscriptionListTypeRepository;

    private PiUser startOfRangeUser;
    private PiUser endOfRangeUser;
    private PiUser outsideRangeUser;
    private PiUser neverSignedInUser;
    private List<Subscription> subscriptions;
    private SubscriptionListType subscriptionListType;

    @BeforeAll
    static void startup() {
        OBJECT_MAPPER.findAndRegisterModules();
    }

    @BeforeEach
    void setup() {
        // Created before the range, but signed in within it
        startOfRangeUser = createUser(BEFORE_RANGE, START_OF_RANGE);
        endOfRangeUser = createUser(BEFORE_RANGE, END_OF_RANGE);
        // Created within the range, but last signed in after it
        outsideRangeUser = createUser(IN_RANGE, OUTSIDE_RANGE);
        // No last signed in date, so filtered on created date
        neverSignedInUser = createUser(IN_RANGE, null);

        subscriptions = subscriptionRepository.saveAll(List.of(
            createSubscription(endOfRangeUser.getUserId(), SearchType.LOCATION_ID, LOCATION_ID),
            createSubscription(endOfRangeUser.getUserId(), SearchType.CASE_NUMBER, CASE_NUMBER)
        ));
        subscriptionListType = subscriptionListTypeRepository.save(new SubscriptionListType(
            endOfRangeUser.getUserId(), List.of("CIVIL_DAILY_CAUSE_LIST"), List.of("ENGLISH")
        ));
    }

    @AfterEach
    void tearDown() {
        subscriptionRepository.deleteAll(subscriptions);
        subscriptionListTypeRepository.delete(subscriptionListType);
        userRepository.deleteAll(List.of(startOfRangeUser, endOfRangeUser, outsideRangeUser, neverSignedInUser));
    }

    private PiUser createUser(LocalDateTime createdDate, LocalDateTime lastSignedInDate) {
        PiUser user = new PiUser();
        user.setEmail("migration-" + UUID.randomUUID() + "@justice.gov.uk");
        user.setProvenanceUserId(UUID.randomUUID().toString());
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setRoles(Roles.VERIFIED);
        user.setForenames("Migration");
        user.setSurname("User");
        user.setCreatedDate(createdDate);
        user.setLastVerifiedDate(createdDate);
        user.setLastSignedInDate(lastSignedInDate);
        return userRepository.save(user);
    }

    private Subscription createSubscription(UUID userId, SearchType searchType, String searchValue) {
        Subscription subscription = new Subscription();
        subscription.setUserId(userId);
        subscription.setSearchType(searchType);
        subscription.setSearchValue(searchValue);
        subscription.setChannel(Channel.EMAIL);
        return subscription;
    }

    private List<UserWithSubscriptions> getUsers(String from, String to) throws Exception {
        MvcResult result = mockMvc.perform(get(ACCOUNT_URL).queryParam(FROM_PARAM, from).queryParam(TO_PARAM, to))
            .andExpect(status().isOk())
            .andReturn();
        return OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(), new TypeReference<>() {});
    }

    private static List<UUID> userIds(List<UserWithSubscriptions> users) {
        return users.stream().map(u -> u.getUser().getUserId()).toList();
    }

    private static UserWithSubscriptions findUser(List<UserWithSubscriptions> users, UUID userId) {
        return users.stream().filter(u -> userId.equals(u.getUser().getUserId())).findFirst().orElseThrow();
    }

    @Test
    void testNoFiltersReturnsAllUsersWithAllFieldsAndSubscriptions() throws Exception {
        MvcResult result = mockMvc.perform(get(ACCOUNT_URL))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = OBJECT_MAPPER.readTree(result.getResponse().getContentAsString());
        JsonNode endOfRangeJson = null;
        for (JsonNode node : json) {
            if (endOfRangeUser.getUserId().toString().equals(node.get("userId").asText())) {
                endOfRangeJson = node;
            }
        }
        assertThat(endOfRangeJson).isNotNull();
        assertThat(endOfRangeJson.get("email").asText()).isEqualTo(endOfRangeUser.getEmail());
        assertThat(endOfRangeJson.get("provenanceUserId").asText()).isEqualTo(endOfRangeUser.getProvenanceUserId());
        assertThat(endOfRangeJson.get("userProvenance").asText()).isEqualTo("PI_AAD");
        assertThat(endOfRangeJson.get("roles").asText()).isEqualTo("VERIFIED");
        assertThat(endOfRangeJson.get("forenames").asText()).isEqualTo("Migration");
        assertThat(endOfRangeJson.get("surname").asText()).isEqualTo("User");
        assertThat(endOfRangeJson.get("createdDate").asText()).isEqualTo("2000-06-01T12:00:00Z");
        assertThat(endOfRangeJson.get("lastVerifiedDate").asText()).isEqualTo("2000-06-01T12:00:00Z");
        assertThat(endOfRangeJson.get("lastSignedInDate").asText()).isEqualTo("2001-01-15T23:59:59Z");

        List<UserWithSubscriptions> users = OBJECT_MAPPER.convertValue(json, new TypeReference<>() {});
        assertThat(userIds(users)).contains(
            startOfRangeUser.getUserId(), endOfRangeUser.getUserId(), outsideRangeUser.getUserId(),
            neverSignedInUser.getUserId()
        );

        UserWithSubscriptions withSubscriptions = findUser(users, endOfRangeUser.getUserId());
        assertThat(withSubscriptions.getSubscriptions())
            .extracting(Subscription::getId)
            .containsExactlyInAnyOrderElementsOf(subscriptions.stream().map(Subscription::getId).toList());
        assertThat(withSubscriptions.getSubscriptionListType().getListType())
            .containsExactly("CIVIL_DAILY_CAUSE_LIST");
        assertThat(withSubscriptions.getSubscriptionListType().getListLanguage()).containsExactly("ENGLISH");
    }

    @Test
    void testUserWithNoSubscriptionsReturnsEmptyList() throws Exception {
        UserWithSubscriptions user = findUser(getUsers(null, null), outsideRangeUser.getUserId());

        assertThat(user.getSubscriptions()).isEmpty();
        assertThat(user.getSubscriptionListType()).isNull();
    }

    @Test
    void testBothFiltersReturnsUsersBetweenDatesInclusive() throws Exception {
        List<UserWithSubscriptions> users = getUsers("2001-01-01", "2001-01-15");

        assertThat(userIds(users)).containsExactlyInAnyOrder(
            startOfRangeUser.getUserId(), endOfRangeUser.getUserId(), neverSignedInUser.getUserId()
        );
        assertThat(findUser(users, endOfRangeUser.getUserId()).getSubscriptions()).hasSize(2);
    }

    @Test
    void testFromFilterOnlyReturnsUsersSignedInOnOrAfterDate() throws Exception {
        List<UUID> userIds = userIds(getUsers("2001-01-15", null));

        assertThat(userIds)
            .contains(endOfRangeUser.getUserId(), outsideRangeUser.getUserId())
            .doesNotContain(startOfRangeUser.getUserId(), neverSignedInUser.getUserId());
    }

    @Test
    void testToFilterOnlyReturnsUsersSignedInOnOrBeforeDate() throws Exception {
        List<UUID> userIds = userIds(getUsers(null, "2001-01-15"));

        assertThat(userIds)
            .contains(startOfRangeUser.getUserId(), endOfRangeUser.getUserId(), neverSignedInUser.getUserId())
            .doesNotContain(outsideRangeUser.getUserId());
    }

    @Test
    void testInvalidDateReturnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(get(ACCOUNT_URL).queryParam(FROM_PARAM, "2001-13-45"))
            .andExpect(status().isBadRequest())
            .andReturn();

        ExceptionResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(),
                                                             ExceptionResponse.class);
        assertThat(response.getMessage()).isEqualTo("Invalid value '2001-13-45' for parameter 'lastSignedInDateFrom'");
    }

    @Test
    void testFromAfterToReturnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(get(ACCOUNT_URL)
                                               .queryParam(FROM_PARAM, "2001-01-16")
                                               .queryParam(TO_PARAM, "2001-01-15"))
            .andExpect(status().isBadRequest())
            .andReturn();

        ExceptionResponse response = OBJECT_MAPPER.readValue(result.getResponse().getContentAsString(),
                                                             ExceptionResponse.class);
        assertThat(response.getMessage()).isEqualTo("lastSignedInDateFrom must not be after lastSignedInDateTo");
    }

    @Test
    @WithMockUser(username = UNAUTHORIZED_USERNAME, authorities = {UNAUTHORIZED_ROLE})
    void testUnauthorisedGetUsersReturnsForbidden() throws Exception {
        mockMvc.perform(get(ACCOUNT_URL))
            .andExpect(status().isForbidden());
    }
}
