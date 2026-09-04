package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUserArchived;
import uk.gov.hmcts.reform.pip.model.report.DeletedAccountMiData;

import java.util.List;

public interface UserArchivedRepository extends JpaRepository<PiUserArchived, Long> {
    @Query("SELECT new uk.gov.hmcts.reform.pip.model.report.DeletedAccountMiData("
        + "userId, provenanceUserId, userProvenance, roles, lastSignedInDate, archivedDate) "
        + "FROM PiUserArchived ")
    List<DeletedAccountMiData> getAccountDataForMi();
}
