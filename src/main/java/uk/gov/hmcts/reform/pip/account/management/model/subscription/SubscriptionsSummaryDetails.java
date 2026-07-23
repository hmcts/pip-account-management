package uk.gov.hmcts.reform.pip.account.management.model.subscription;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SubscriptionsSummaryDetails {
    /**
     * Case URN subscription list.
     *
     * @deprecated Use caseNumber instead.
     */
    @JsonProperty("CASE_URN")
    @Deprecated(since = "1.0", forRemoval = true)
    private List<String> caseUrn = new ArrayList<>();
    @JsonProperty("CASE_NUMBER")
    private List<String> caseNumber = new ArrayList<>();
    @JsonProperty("CASE_NAME")
    private List<String> caseName = new ArrayList<>();
    @JsonProperty("LOCATION_ID")
    private List<String> locationId = new ArrayList<>();

    /**
     * Adds a case URN value to the list.
     *
     * @deprecated Use addToCaseNumber instead.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    public void addToCaseUrn(String caseUrn) {
        this.caseUrn.add(caseUrn);
    }

    public void addToCaseNumber(String caseNumber) {
        this.caseNumber.add(caseNumber);
    }

    public void addToCaseName(String caseName) {
        this.caseName.add(caseName);
    }

    public void addToLocationId(String locationId) {
        this.locationId.add(locationId);
    }
}
