package uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription;

import lombok.Data;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Model which represents a list type subscription.
 */
@Data
public class ListTypeSubscription {

    private UUID subscriptionId;
    private String listType;
    private LocalDateTime dateAdded;
    private Channel channel;
}
