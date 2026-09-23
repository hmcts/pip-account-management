package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUserArchived;
import uk.gov.hmcts.reform.pip.model.report.DeletedAccountMiData;

import java.util.List;
import java.util.UUID;

public interface UserArchivedRepository extends JpaRepository<PiUserArchived, Long> {
    void deleteByUserId(UUID userId);

    @Query(value = "SELECT * FROM pi_user_archived "
        + " WHERE CAST(archived_date AS DATE) <= CURRENT_DATE - (interval '1' day) * :days ", nativeQuery = true)
    List<PiUserArchived> findOutdatedArchivedAccounts(@Param("days") int archivedAccountDeletionDays);

    @Query("SELECT new uk.gov.hmcts.reform.pip.model.report.DeletedAccountMiData("
        + "userId, provenanceUserId, userProvenance, roles, lastSignedInDate, archivedDate) "
        + "FROM PiUserArchived ")
    List<DeletedAccountMiData> getAccountDataForMi();
}
