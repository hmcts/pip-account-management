package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pip.account.management.model.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.Roles;
import uk.gov.hmcts.reform.pip.account.management.model.UserProvenances;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<PiUser, Long> {
    @Query(value = "SELECT * FROM pi_user WHERE provenance_user_id=:provUserId AND user_provenance=:userProv",
        nativeQuery = true)
    List<PiUser> findExistingByProvenanceId(@Param("provUserId") String provenanceUserId,
                                            @Param("userProv") String userProvenance);

    Optional<PiUser> findByUserId(UUID userId);

    @Query(value = "SELECT cast(USER_ID as text), PROVENANCE_USER_ID, USER_PROVENANCE, ROLES FROM pi_user",
        nativeQuery = true)
    List<String> getAccManDataForMI();


    @Query(value = "SELECT * FROM pi_user WHERE CAST(last_verified_date AS DATE) = CURRENT_DATE - (interval '1' day)"
        + " * :daysAgo AND roles = 'VERIFIED'", nativeQuery = true)
    List<PiUser> findVerifiedUsersByLastVerifiedDate(@Param("daysAgo") int daysSinceLastVerified);

    @Query(value = "SELECT * FROM pi_user WHERE CAST(last_signed_in_date AS DATE) = CURRENT_DATE - (interval '1' day)"
        + " * :daysAgo AND roles <> 'VERIFIED' AND user_provenance = 'PI_AAD'", nativeQuery = true)
    List<PiUser> findAadAdminUsersByLastSignedInDate(@Param("daysAgo") int daysSinceLastSignedIn);

    @Query(value = "SELECT * FROM pi_user WHERE CAST(last_signed_in_date AS DATE) = CURRENT_DATE - (interval '1' day)"
        + " * :daysAgo AND (user_provenance = 'CFT_IDAM' OR user_provenance = 'CRIME_IDAM')", nativeQuery = true)
    List<PiUser> findIdamUsersByLastSignedInDate(@Param("daysAgo") int daysSinceLastSignedIn);

    Optional<PiUser> findByEmail(String email);

    Optional<PiUser> findByProvenanceUserIdAndUserProvenance(String provenanceUserId, UserProvenances userProvenance);

    List<PiUser> findAllByRolesIn(List<Roles> role);
}
