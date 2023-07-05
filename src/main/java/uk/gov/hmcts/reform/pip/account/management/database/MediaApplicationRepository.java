package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplication;
import uk.gov.hmcts.reform.pip.account.management.model.MediaApplicationStatus;

import java.util.List;
import java.util.UUID;

public interface MediaApplicationRepository extends JpaRepository<MediaApplication, UUID> {

    List<MediaApplication> findByStatus(MediaApplicationStatus status);

    List<MediaApplication> findAllByEmailStartingWithIgnoreCase(@Param("prefix") String prefix);

    void deleteByIdIn(List<UUID> id);
}
