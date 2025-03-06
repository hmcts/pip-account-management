package uk.gov.hmcts.reform.pip.account.management.model.subscription;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class SubscriptionListType {

    /**
     * Unique ListType subscription ID.
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
    private UUID userId;

    @Valid
    @Type(ListArrayType.class)
    @Column(name = "list_type", columnDefinition = "text[]")
    private List<String> listType;

    @Valid
    @Type(ListArrayType.class)
    @Column(name = "list_language", columnDefinition = "text[]")
    private List<String> listLanguage;

    public SubscriptionListType(UUID userId, List<String> listType,
                                List<String> listLanguage) {
        this.userId = userId;
        this.listType = listType;
        this.listLanguage = listLanguage;

    }
}
