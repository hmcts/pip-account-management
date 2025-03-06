package uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class LocationSubscription {

    private UUID subscriptionId;
    private String locationName;
    private String locationId;
    private List<String> listType;
    private List<String> listLanguage;
    private LocalDateTime dateAdded;
}
