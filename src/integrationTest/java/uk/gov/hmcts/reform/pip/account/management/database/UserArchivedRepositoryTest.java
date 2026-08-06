package uk.gov.hmcts.reform.pip.account.management.database;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUserArchived;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.report.DeletedAccountMiData;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("integration-jpa")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserArchivedRepositoryTest {
    private static final UUID USER_ID1 = UUID.randomUUID();
    private static final UUID USER_ID2 = UUID.randomUUID();
    private static final String PROVENANCE_USER_ID1 = UUID.randomUUID().toString();
    private static final String PROVENANCE_USER_ID2 = UUID.randomUUID().toString();
    private static final LocalDateTime LAST_SIGNED_IN_DATE1 = LocalDateTime.now().minus(1, ChronoUnit.MONTHS);
    private static final LocalDateTime LAST_SIGNED_IN_DATE2 = LocalDateTime.now().minus(5, ChronoUnit.WEEKS);
    private static final LocalDateTime ARCHIVED_DATE1 = LocalDateTime.now().minus(1, ChronoUnit.DAYS);
    private static final LocalDateTime ARCHIVED_DATE2 = LocalDateTime.now().minus(2, ChronoUnit.DAYS);

    @Autowired
    private UserArchivedRepository userArchivedRepository;

    @BeforeAll
    void setup() {
        PiUserArchived user1 = new PiUserArchived();
        user1.setUserId(USER_ID1);
        user1.setProvenanceUserId(PROVENANCE_USER_ID1);
        user1.setUserProvenance(UserProvenances.PI_AAD);
        user1.setRoles(Roles.VERIFIED);
        user1.setLastSignedInDate(LAST_SIGNED_IN_DATE1);
        user1.setArchivedDate(ARCHIVED_DATE1);

        PiUserArchived user2 = new PiUserArchived();
        user2.setUserId(USER_ID2);
        user2.setProvenanceUserId(PROVENANCE_USER_ID2);
        user2.setUserProvenance(UserProvenances.CFT_IDAM);
        user2.setRoles(Roles.VERIFIED);
        user2.setLastSignedInDate(LAST_SIGNED_IN_DATE2);
        user2.setArchivedDate(ARCHIVED_DATE2);

        userArchivedRepository.saveAll(List.of(user1, user2));
    }

    @Test
    void shouldGetDeletedAccountMiData() {
        List<DeletedAccountMiData> accountMiData = userArchivedRepository.getAccountDataForMi();

        assertThat(accountMiData)
            .as("Returned count does not match")
            .hasSize(2);

        assertThat(accountMiData)
            .as("Returned deleted account MI data does not match")
            .anyMatch(account -> USER_ID1.equals(account.getUserId())
                && PROVENANCE_USER_ID1.equals(account.getProvenanceUserId())
                && UserProvenances.PI_AAD.equals(account.getUserProvenance())
                && Roles.VERIFIED.equals(account.getRoles())
                && LAST_SIGNED_IN_DATE1.equals(account.getLastSignedInDate())
                && ARCHIVED_DATE1.equals(account.getDeletedDate()));
    }
}
