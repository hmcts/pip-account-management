package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pip.account.management.model.MediaAndLegalApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaLegalApplicationStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MediaLegalApplicationRepository extends JpaRepository<MediaAndLegalApplication, UUID> {

    List<MediaAndLegalApplication> findByStatus(MediaLegalApplicationStatus status);

    Optional<MediaAndLegalApplication> findByEmail(String email);
}
