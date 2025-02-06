package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * This JPA interface allows us to specify specific find methods for the database and it should
 * create the required functionality for us.
 */
@Repository
@SuppressWarnings({"PMD.TooManyMethods"})
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findById(UUID id);

    List<Subscription> findByIdIn(List<UUID> id);

    List<Subscription> findByUserId(String userId);

    void deleteById(UUID id);

    void deleteByIdIn(List<UUID> id);

    @Query(value = "SELECT * FROM Subscription "
        + "WHERE search_type = :search_type "
        + "AND search_value = :search_value "
        + "AND :search_type <> 'LOCATION_ID'",
        nativeQuery = true)
    List<Subscription> findSubscriptionsBySearchValue(@Param("search_type") String searchType,
                                                      @Param("search_value") String searchValue);

    @Query(value = "SELECT cast(id as text), channel, search_type, user_id, location_name, created_date "
        + "FROM Subscription", nativeQuery = true)
    List<String> getAllSubsDataForMi();

    @Query(value = "SELECT cast(ID as text), search_value, channel, user_id, location_name, created_date "
        + "FROM Subscription WHERE search_type ='LOCATION_ID'", nativeQuery = true)
    List<String> getLocalSubsDataForMi();

    @Query(value = "SELECT s.* FROM Subscription s "
        + "INNER JOIN Subscription_List_Type sl "
        + "ON s.user_id = sl.user_id "
        + "WHERE s.search_type = 'LOCATION_ID' "
        + "AND s.search_value = :search_value "
        + "AND (ARRAY_LENGTH(sl.list_type, 1) IS NULL OR  sl.list_type && string_to_array(:list_type, ',')) "
        + "AND sl.list_language && string_to_array(:list_language, ',')",
        nativeQuery = true)
    List<Subscription> findSubscriptionsByLocationSearchValue(@Param("search_value") String searchValue,
                                                              @Param("list_type") String listType,
                                                              @Param("list_language") String listLanguage);

    @Transactional
    void deleteAllByUserId(String userId);

    @Query(value = "SELECT * FROM Subscription "
        + "WHERE search_value = :search_value "
        + "AND search_type = 'LOCATION_ID'",
        nativeQuery = true)
    List<Subscription> findSubscriptionsByLocationId(@Param("search_value") String searchValue);

    @Query(value = "SELECT * FROM Subscription "
        + "WHERE user_id = :user_id "
        + "AND search_type = 'LOCATION_ID'",
        nativeQuery = true)
    List<Subscription> findLocationSubscriptionsByUserId(@Param("user_id") String userId);

    List<Subscription> findAllByLocationNameStartingWithIgnoreCase(@Param("prefix") String prefix);

    @Modifying
    @Transactional
    @Query(value = "REFRESH MATERIALIZED VIEW sdp_mat_view_subscription", nativeQuery = true)
    void refreshSubscriptionView();
}
