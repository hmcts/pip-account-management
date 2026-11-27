package uk.gov.hmcts.reform.pip.account.management.model.account;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.Subscription;
import uk.gov.hmcts.reform.pip.account.management.model.subscription.SubscriptionListType;

import java.util.List;

/**
 * A user with all of their fields, alongside their subscriptions and subscription list type configuration.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserWithSubscriptions {

    @JsonUnwrapped
    private PiUser user;

    private List<Subscription> subscriptions;

    private SubscriptionListType subscriptionListType;
}
