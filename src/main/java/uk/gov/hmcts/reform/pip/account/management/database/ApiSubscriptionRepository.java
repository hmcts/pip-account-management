package uk.gov.hmcts.reform.pip.account.management.database;

import org.springframework.data.jpa.repository.JpaRepository;
import uk.gov.hmcts.reform.pip.account.management.model.thirdparty.ApiSubscription;
import uk.gov.hmcts.reform.pip.model.publication.ListType;
import uk.gov.hmcts.reform.pip.model.publication.Sensitivity;

import java.util.List;
import java.util.UUID;

public interface ApiSubscriptionRepository extends JpaRepository<ApiSubscription, UUID> {
    List<ApiSubscription> findAllByUserId(UUID userId);

    void deleteAllByUserId(UUID userId);

    List<ApiSubscription> findByListTypeAndSensitivityIn(ListType listType, List<Sensitivity> sensitivities);
}
