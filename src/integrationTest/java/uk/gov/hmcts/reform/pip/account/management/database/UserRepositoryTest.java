package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserRepositoryTest {
    private static final String PROVENANCE_USER_ID1 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID2 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID3 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID4 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID5 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID6 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID7 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID8 = UUID.randomUUID().toString();
    private static final String EMAIL1 = "TestUser1@justice.gov.uk";
    private static final String EMAIL2 = "TestUser2@justice.gov.uk";
    private static final String EMAIL3 = "TestUser3@justice.gov.uk";
    private static final String EMAIL4 = "TestUser4@justice.gov.uk";
    private static final String EMAIL5 = "TestUser5@justice.gov.uk";
    private static final String EMAIL6 = "TestUser6@justice.gov.uk";
    private static final String EMAIL7 = "TestUser7@justice.gov.uk";
    private static final String EMAIL8 = "TestUser8@justice.gov.uk";

    private static final LocalDateTime TIMESTAMP_NOW = LocalDateTime.now();
    private static final int DAYS = 5;

    private static final String USER_MATCHED_MESSAGE = "User does not match";
    private static final String USER_EMPTY_MESSAGE = "User is not empty";

    private UUID userId1;
    private UUID userId2;
    private UUID userId3;
    private UUID userId4;
    private UUID userId5;
    private UUID userId6;
    private UUID userId7;
    private UUID userId8;

    @Autowired
    UserRepository userRepository;

    @BeforeAll
    void setup() {
        PiUser user1 = new PiUser();
        user1.setEmail(EMAIL1);
        user1.setProvenanceUserId(PROVENANCE_USER_ID1);
        user1.setUserProvenance(UserProvenances.PI_AAD);
        user1.setRoles(Roles.VERIFIED);
        user1.setLastVerifiedDate(TIMESTAMP_NOW.minusDays(DAYS));
        userId1 = userRepository.save(user1).getUserId();

        PiUser user2 = new PiUser();
        user2.setEmail(EMAIL2);
        user2.setProvenanceUserId(PROVENANCE_USER_ID2);
        user2.setUserProvenance(UserProvenances.PI_AAD);
        user2.setRoles(Roles.VERIFIED);
        user2.setLastVerifiedDate(TIMESTAMP_NOW.minusDays(DAYS + 1));
        userId2 = userRepository.save(user2).getUserId();

        PiUser user3 = new PiUser();
        user3.setEmail(EMAIL3);
        user3.setProvenanceUserId(PROVENANCE_USER_ID3);
        user3.setUserProvenance(UserProvenances.PI_AAD);
        user3.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        user3.setLastSignedInDate(TIMESTAMP_NOW.minusDays(DAYS));
        user3.setCreatedDate(TIMESTAMP_NOW.minusDays(DAYS));
        userId3 = userRepository.save(user3).getUserId();

        PiUser user4 = new PiUser();
        user4.setEmail(EMAIL4);
        user4.setProvenanceUserId(PROVENANCE_USER_ID4);
        user4.setUserProvenance(UserProvenances.SSO);
        user4.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        user4.setLastSignedInDate(TIMESTAMP_NOW.minusDays(DAYS));
        userId4 = userRepository.save(user4).getUserId();

        PiUser user5 = new PiUser();
        user5.setEmail(EMAIL5);
        user5.setProvenanceUserId(PROVENANCE_USER_ID5);
        user5.setUserProvenance(UserProvenances.CFT_IDAM);
        user5.setRoles(Roles.VERIFIED);
        user5.setLastSignedInDate(TIMESTAMP_NOW.minusDays(DAYS));
        userId5 = userRepository.save(user5).getUserId();

        PiUser user6 = new PiUser();
        user6.setEmail(EMAIL6);
        user6.setProvenanceUserId(PROVENANCE_USER_ID6);
        user6.setUserProvenance(UserProvenances.CFT_IDAM);
        user6.setRoles(Roles.VERIFIED);
        user6.setLastSignedInDate(TIMESTAMP_NOW.minusDays(DAYS + 1));
        userId6 = userRepository.save(user6).getUserId();

        PiUser user7 = new PiUser();
        user7.setEmail(EMAIL7);
        user7.setProvenanceUserId(PROVENANCE_USER_ID7);
        user7.setUserProvenance(UserProvenances.CRIME_IDAM);
        user7.setRoles(Roles.VERIFIED);
        user7.setLastSignedInDate(TIMESTAMP_NOW.minusDays(DAYS));
        userId7 = userRepository.save(user7).getUserId();

        PiUser user8 = new PiUser();
        user8.setEmail(EMAIL8);
        user8.setProvenanceUserId(PROVENANCE_USER_ID8);
        user8.setUserProvenance(UserProvenances.CRIME_IDAM);
        user8.setRoles(Roles.VERIFIED);
        user8.setLastSignedInDate(TIMESTAMP_NOW.minusDays(DAYS + 1));
        userId8 = userRepository.save(user8).getUserId();
    }

    @AfterAll
    void shutdown() {
        userRepository.deleteAll();
    }

    @Test
    void shouldFindUserByProvenanceId() {
        assertThat(userRepository.findExistingByProvenanceId(PROVENANCE_USER_ID1, UserProvenances.PI_AAD.name()))
            .as(USER_MATCHED_MESSAGE)
            .hasSize(1)
            .extracting(PiUser::getUserId)
            .containsExactly(userId1);
    }

    @Test
    void shouldNotFindUserByProvenanceIdIfUserProvenanceMismatch() {
        assertThat(userRepository.findExistingByProvenanceId(PROVENANCE_USER_ID1, UserProvenances.CFT_IDAM.name()))
            .as(USER_EMPTY_MESSAGE)
            .isEmpty();
    }

    @Test
    void shouldFindVerifiedUsersForNotificationByLastVerifiedDate() {
        assertThat(userRepository.findVerifiedUsersForNotificationByLastVerifiedDate(DAYS))
            .as(USER_MATCHED_MESSAGE)
            .hasSize(1)
            .extracting(PiUser::getUserId)
            .containsExactly(userId1);
    }

    @Test
    void shouldFindVerifiedUsersForDeletionByLastVerifiedDate() {
        assertThat(userRepository.findVerifiedUsersForNotificationByLastVerifiedDate(DAYS))
            .as(USER_MATCHED_MESSAGE)
            .hasSize(2)
            .extracting(PiUser::getUserId)
            .containsExactly(userId1, userId2);
    }

    @Test
    void shouldFindAdminUsersForNotificationByLastSignedInDate() {
        assertThat(userRepository.findAdminUsersForNotificationByLastSignedInDate(DAYS))
            .as(USER_MATCHED_MESSAGE)
            .hasSize(1)
            .extracting(PiUser::getUserId)
            .containsExactly(userId3);
    }

    @Test
    void shouldFindAdminUsersForDeletionByLastSignedInDate() {
        assertThat(userRepository.findAdminUsersForDeletionByLastSignedInDate(DAYS, DAYS))
            .as(USER_MATCHED_MESSAGE)
            .hasSize(2)
            .extracting(PiUser::getUserId)
            .containsExactlyInAnyOrder(userId3, userId4);
    }

    @Test
    void shouldFindIdamUsersForNotificationByLastSignedInDate() {
        assertThat(userRepository.findIdamUsersForNotificationByLastSignedInDate(DAYS, DAYS))
            .as(USER_MATCHED_MESSAGE)
            .hasSize(2)
            .extracting(PiUser::getUserId)
            .containsExactlyInAnyOrder(userId5, userId7);
    }

    @Test
    void shouldFindIdamUsersForDeletionByLastSignedInDate() {
        assertThat(userRepository.findIdamUsersForNotificationByLastSignedInDate(DAYS, DAYS))
            .as(USER_MATCHED_MESSAGE)
            .hasSize(2)
            .extracting(PiUser::getUserId)
            .containsExactlyInAnyOrder(userId5, userId6, userId7, userId8);
    }

    @Test
    void shouldFindByUserIdPageable() {
        Pageable pageable = PageRequest.of(0, 25);
        Page<PiUser> page = userRepository.findByUserIdPageable(userId1.toString(), pageable);

        assertThat(page.getContent())
            .hasSize(1)
            .first()
            .extracting(PiUser::getUserId)
            .isEqualTo(userId1);
    }

    @Test
    void shouldGetMiData() {
        List<AccountMiData> accountMiData = userRepository.getAccountDataForMi();

        assertThat(accountMiData)
            .as("Returned account MI data must match user object")
            .anyMatch(account -> userId3.equals(account.getUserId())
                && PROVENANCE_USER_ID3.equals(account.getProvenanceUserId())
                && UserProvenances.PI_AAD.equals(account.getUserProvenance())
                && Roles.INTERNAL_ADMIN_CTSC.equals(account.getRoles())
                && TIMESTAMP_NOW.minusDays(DAYS).truncatedTo(ChronoUnit.SECONDS)
                .equals(account.getLastSignedInDate().truncatedTo(ChronoUnit.SECONDS))
                && TIMESTAMP_NOW.minusDays(DAYS).truncatedTo(ChronoUnit.SECONDS)
                .equals(account.getCreatedDate().truncatedTo(ChronoUnit.SECONDS)));
    }
}
