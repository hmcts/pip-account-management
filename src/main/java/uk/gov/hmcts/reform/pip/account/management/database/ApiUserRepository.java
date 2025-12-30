package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiUser;

import java.util.Optional;
import java.util.UUID;

public interface ApiUserRepository extends JpaRepository<ApiUser, UUID> {
    Optional<ApiUser> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
