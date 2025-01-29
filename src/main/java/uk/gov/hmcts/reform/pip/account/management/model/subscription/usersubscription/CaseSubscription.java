package uk.gov.hmcts.reform.pip.account.management.model.subscription.usersubscription;

import lombok.Data;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class CaseSubscription {

    private UUID subscriptionId;
    private String caseName;
    private String partyNames;
    private String caseNumber;
    private SearchType searchType;
    private String urn;
    private LocalDateTime dateAdded;
}
