package uk.gov.hmcts.reform.pip.account.management.model;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class UserCommunication {
    private static final String TEXT_ARRAY = "text[]";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private AudienceType audienceType;

    private String message;

    private String welshMessage;

    @Type(ListArrayType.class)
    @Column(name = "user_provenances", columnDefinition = TEXT_ARRAY)
    private List<String> userProvenances;

    @Type(ListArrayType.class)
    @Column(name = "user_roles", columnDefinition = TEXT_ARRAY)
    private List<String> userRoles;

    @Type(ListArrayType.class)
    @Column(name = "sub-jurisdictions", columnDefinition = TEXT_ARRAY)
    private List<String> subJurisdictions;

    @Type(ListArrayType.class)
    @Column(name = "venues", columnDefinition = "integer[]")
    private List<Integer> venues;

    @Schema(hidden = true)
    @CreatedDate
    private LocalDateTime createdDate;

    @Schema(hidden = true)
    @LastModifiedDate
    private LocalDateTime lastUpdatedDate;
}
