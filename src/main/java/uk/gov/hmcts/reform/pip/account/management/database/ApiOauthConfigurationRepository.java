package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiOauthConfiguration;

import java.util.Optional;
import java.util.UUID;

public interface ApiOauthConfigurationRepository extends JpaRepository<ApiOauthConfiguration, UUID> {
    Optional<ApiOauthConfiguration> findByUserId(UUID userId);

    void deleteByUserId(UUID userId);
}
