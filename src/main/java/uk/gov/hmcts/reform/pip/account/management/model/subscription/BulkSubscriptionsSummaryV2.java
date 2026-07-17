package uk.gov.hmcts.reform.pip.account.management.model.subscription;

import lombok.Data;
import uk.gov.hmcts.reform.pip.model.publication.Artefact;

import java.util.ArrayList;
import java.util.List;

@Data
public class BulkSubscriptionsSummaryV2 {
    private Artefact artefact;

    List<SubscriptionsSummary> subscriptionEmails = new ArrayList<>();

    public void addSubscriptionEmail(SubscriptionsSummary subscriptionsSummary) {
        subscriptionEmails.add(subscriptionsSummary);
    }
}
