package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;

import java.util.List;
import java.util.UUID;

public interface MediaLegalApplicationRepository extends JpaRepository<MediaAndLegalApplication, UUID> {

    @Query(value = "SELECT * FROM media_and_legal_application WHERE status = :status", nativeQuery = true)
    List<MediaAndLegalApplication> findByStatus(@Param("status") String status);
}
