package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionListTypeRepository extends JpaRepository<SubscriptionListType, Long> {

    Optional<SubscriptionListType> findByUserId(UUID userId);

    @Transactional
    void deleteByUserId(UUID userId);
}
