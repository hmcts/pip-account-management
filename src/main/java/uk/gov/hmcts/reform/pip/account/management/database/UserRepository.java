package uk.gov.hmcts.reform.pip.account.management.database;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.report.AccountMiData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<PiUser, Long> {
    @Query(value = "SELECT * FROM pi_user WHERE provenance_user_id=:provUserId AND user_provenance=:userProv",
        nativeQuery = true)
    List<PiUser> findExistingByProvenanceId(@Param("provUserId") String provenanceUserId,
                                            @Param("userProv") String userProvenance);

    Optional<PiUser> findByUserId(UUID userId);

    @Query("SELECT new uk.gov.hmcts.reform.pip.model.report.AccountMiData("
        + "userId, provenanceUserId, userProvenance, roles, createdDate, lastSignedInDate) "
        + "FROM PiUser")
    List<AccountMiData> getAccountDataForMi();

    @Query(value = "SELECT * FROM pi_user WHERE CAST(last_verified_date AS DATE) = CURRENT_DATE - (interval '1' day)"
        + " * :daysAgo AND user_provenance = 'PI_AAD' AND roles = 'VERIFIED'", nativeQuery = true)
    List<PiUser> findVerifiedUsersForNotificationByLastVerifiedDate(@Param("daysAgo") int daysSinceLastVerified);

    @Query(value = "SELECT * FROM pi_user WHERE CAST(last_verified_date AS DATE) <= CURRENT_DATE - (interval '1' day)"
        + " * :daysAgo AND user_provenance = 'PI_AAD' AND roles = 'VERIFIED'", nativeQuery = true)
    List<PiUser> findVerifiedUsersForDeletionByLastVerifiedDate(@Param("daysAgo") int daysSinceLastVerified);

    @Query(value = "SELECT * FROM pi_user WHERE (user_provenance = 'PI_AAD' AND roles <> 'VERIFIED' AND "
        + "CAST(last_signed_in_date AS DATE) <= CURRENT_DATE - (interval '1' day) * :aadDays) OR "
        + "(user_provenance = 'SSO' AND "
        + "CAST(last_signed_in_date AS DATE) <= CURRENT_DATE - (interval '1' day) * :ssoDays)", nativeQuery = true)
    List<PiUser> findAdminUsersForDeletionByLastSignedInDate(@Param("aadDays") int aadNumberOfDays,
                                                             @Param("ssoDays") int ssoNumberOfDays);

    @Query(value = "SELECT * FROM pi_user WHERE (CAST(last_signed_in_date AS DATE) = CURRENT_DATE - (interval '1' day)"
        + " * :cftDaysAgo AND user_provenance = 'CFT_IDAM') "
        + " OR (CAST(last_signed_in_date AS DATE) = CURRENT_DATE - (interval '1' day)"
        + " * :crimeDaysAgo AND user_provenance = 'CRIME_IDAM') ", nativeQuery = true)
    List<PiUser> findIdamUsersForNotificationByLastSignedInDate(@Param("cftDaysAgo") int cftDaysSinceLastSignedIn,
                                                                @Param("crimeDaysAgo") int crimeDaysSinceLastSignedIn);

    @Query(value = "SELECT * FROM pi_user WHERE (CAST(last_signed_in_date AS DATE) <= CURRENT_DATE - (interval '1' day)"
        + " * :cftDaysAgo AND user_provenance = 'CFT_IDAM') "
        + " OR (CAST(last_signed_in_date AS DATE) <= CURRENT_DATE - (interval '1' day)"
        + " * :crimeDaysAgo AND user_provenance = 'CRIME_IDAM') ", nativeQuery = true)
    List<PiUser> findIdamUsersForDeletionByLastSignedInDate(@Param("cftDaysAgo") int cftDaysSinceLastSignedIn,
                                                            @Param("crimeDaysAgo") int crimeDaysSinceLastSignedIn);

    Optional<PiUser> findByEmailIgnoreCaseAndUserProvenanceAndRolesIn(String email, UserProvenances userProvenances,
                                                                      List<Roles> roles);

    List<PiUser> findByRoles(Roles role);

    Optional<PiUser> findByEmailAndUserProvenance(String email, UserProvenances userProvenances);

    Optional<PiUser> findByProvenanceUserIdAndUserProvenance(String provenanceUserId, UserProvenances userProvenance);

    List<PiUser> findAllByUserProvenance(UserProvenances userProvenances);

    Page<PiUser> findAllByEmailLikeIgnoreCaseAndUserProvenanceInAndRolesInAndProvenanceUserIdLike(
        String email,
        List<UserProvenances> provenance,
        List<Roles> roles,
        String provenanceUserId,
        Pageable pageable);

    @Query(value = "SELECT * FROM pi_user WHERE CAST(user_id AS TEXT) = :userId", nativeQuery = true)
    Page<PiUser> findByUserIdPageable(@Param("userId") String userId, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW sdp_mat_view_pi_user", nativeQuery = true)
    void refreshAccountView();

}
