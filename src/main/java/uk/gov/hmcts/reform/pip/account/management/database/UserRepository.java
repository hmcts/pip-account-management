package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<PiUser, Long> {

    @Query(value = "SELECT * FROM pi_user WHERE provenance_user_id=:provUserId AND user_provenance=:userProv",
        nativeQuery = true)
    List<PiUser> findExistingByProvenanceId(@Param("provUserId") String provenanceUserId,
                                            @Param("userProv") String userProvenance);

    Optional<PiUser> findByUserId(UUID userId);
}
