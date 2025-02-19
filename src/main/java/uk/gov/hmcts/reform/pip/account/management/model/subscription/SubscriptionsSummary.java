package uk.gov.hmcts.reform.pip.account.management.model.subscription;

import lombok.Data;

@Data
public class SubscriptionsSummary {
    private String email;
    private SubscriptionsSummaryDetails subscriptions;
}
