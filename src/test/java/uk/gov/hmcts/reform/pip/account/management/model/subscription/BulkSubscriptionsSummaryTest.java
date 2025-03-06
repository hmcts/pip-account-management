package uk.gov.hmcts.reform.pip.account.management.model.subscription;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BulkSubscriptionsSummaryTest {

    @Test
    void testBulkSubscriptionsSummary() {
        BulkSubscriptionsSummary bulkSubscriptionsSummary = new BulkSubscriptionsSummary();
        SubscriptionsSummary subscriptionsSummary = new SubscriptionsSummary();
        bulkSubscriptionsSummary.addSubscriptionEmail(subscriptionsSummary);

        assertEquals(1, bulkSubscriptionsSummary.getSubscriptionEmails().size(),
                     "Incorrect size of subscription emails");
        assertEquals(subscriptionsSummary, bulkSubscriptionsSummary.getSubscriptionEmails().get(0),
                     "Incorrect content of subscription emails");
    }

}
