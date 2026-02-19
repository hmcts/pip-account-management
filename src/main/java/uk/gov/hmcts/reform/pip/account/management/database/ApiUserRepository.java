package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApiUserRepository extends JpaRepository<ApiUser, UUID> {
    Optional<ApiUser> findByUserId(UUID userId);

    List<ApiUser> findAllByNameStartingWithIgnoreCase(@Param("prefix") String prefix);

    void deleteByUserId(UUID userId);
}
