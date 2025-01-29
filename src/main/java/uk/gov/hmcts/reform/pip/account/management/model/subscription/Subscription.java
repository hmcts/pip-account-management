package uk.gov.hmcts.reform.pip.account.management.model.subscription;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.model.subscription.Channel;
import uk.gov.hmcts.reform.pip.model.subscription.SearchType;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
@JsonPropertyOrder({"id", "channel", "searchType", "searchValue", "userID"})
public class Subscription {

    /**
     * Unique subscription ID.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    @Schema(hidden = true)
    private UUID id;

    /**
     *  P&I user id.
     */
    @Valid
    @NotNull
    private String userId;

    @NotNull
    @Enumerated(EnumType.STRING)
    private SearchType searchType;

    @NotNull
    @Valid
    private String searchValue;

    @NotNull
    @Enumerated(EnumType.STRING)
    private Channel channel;

    @Schema(hidden = true)
    private LocalDateTime createdDate;

    @Valid
    private String caseNumber;

    @Valid
    private String caseName;

    @Valid
    private String partyNames;

    @Valid
    private String urn;

    @Valid
    private String locationName;

    @Valid
    private LocalDateTime lastUpdatedDate;

    public Subscription(uk.gov.hmcts.reform.pip.model.subscription.Subscription dto) {
        this.id = dto.getId();
        this.userId = dto.getUserId();
        this.searchType = dto.getSearchType();
        this.searchValue = dto.getSearchValue();
        this.channel = dto.getChannel();
        this.createdDate = dto.getCreatedDate();
        this.caseNumber = dto.getCaseNumber();
        this.caseName = dto.getCaseName();
        this.partyNames = dto.getPartyNames();
        this.urn = dto.getUrn();
        this.locationName = dto.getLocationName();
        this.lastUpdatedDate = dto.getLastUpdatedDate();
    }

    public uk.gov.hmcts.reform.pip.model.subscription.Subscription toDto() {
        uk.gov.hmcts.reform.pip.model.subscription.Subscription dto =
            new uk.gov.hmcts.reform.pip.model.subscription.Subscription();
        dto.setSearchValue(this.searchValue);
        dto.setChannel(this.channel);
        dto.setUserId(this.userId);
        dto.setSearchType(this.searchType);
        dto.setId(this.id);
        dto.setCreatedDate(this.createdDate);
        dto.setCaseNumber(this.caseNumber);
        dto.setCaseName(this.caseName);
        dto.setPartyNames(this.partyNames);
        dto.setUrn(this.urn);
        dto.setLocationName(this.locationName);
        dto.setLastUpdatedDate(this.lastUpdatedDate);
        return dto;
    }
}
