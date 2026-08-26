package uk.gov.hmcts.reform.pip.account.management.model.account;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

class PiUserArchivedTest {
    private static final UUID USER_ID = UUID.randomUUID();
    private static final String PROVENANCE_USER_ID = UUID.randomUUID().toString();
    private static final String EMAIL = "test@test.com";
    private static final LocalDateTime LAST_SIGNED_IN_DATE = LocalDateTime.now().minusMonths(1);

    @Test
    void testPiUserArchivedCreation() {
        PiUser user = new PiUser();
        user.setUserId(USER_ID);
        user.setProvenanceUserId(PROVENANCE_USER_ID);
        user.setUserProvenance(UserProvenances.PI_AAD);
        user.setEmail(EMAIL);
        user.setRoles(Roles.VERIFIED);
        user.setLastSignedInDate(LAST_SIGNED_IN_DATE);

        PiUserArchived archivedUser = new PiUserArchived(user);

        SoftAssertions softly = new SoftAssertions();
        softly.assertThat(archivedUser.getUserId()).isEqualTo(USER_ID);
        softly.assertThat(archivedUser.getProvenanceUserId()).isEqualTo(PROVENANCE_USER_ID);
        softly.assertThat(archivedUser.getUserProvenance()).isEqualTo(UserProvenances.PI_AAD);
        softly.assertThat(archivedUser.getEmail()).isEqualTo(EMAIL);
        softly.assertThat(archivedUser.getRoles()).isEqualTo(Roles.VERIFIED);
        softly.assertThat(archivedUser.getLastSignedInDate()).isEqualTo(LAST_SIGNED_IN_DATE);
        softly.assertThat(archivedUser.getArchivedDate().toLocalDate()).isEqualTo(LocalDate.now());
        softly.assertAll();
    }
}
