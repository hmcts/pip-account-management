package uk.gov.hmcts.reform.pip.account.management.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubscriptionsSummaryDetails {
    @JsonProperty("CASE_URN")
    private List<String> caseUrn = new ArrayList<>();
    @JsonProperty("CASE_NUMBER")
    private List<String> caseNumber = new ArrayList<>();
    @JsonProperty("LOCATION_ID")
    private List<String> locationId = new ArrayList<>();

    public void addToCaseUrn(String caseUrn) {
        this.caseUrn.add(caseUrn);
    }

    public void addToCaseNumber(String caseNumber) {
        this.caseNumber.add(caseNumber);
    }

    public void addToLocationId(String locationId) {
        this.locationId.add(locationId);
    }
}
