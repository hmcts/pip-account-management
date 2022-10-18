package uk.gov.hmcts.reform.pip.account.management.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.PiEmailConditionalValidation;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidProvenanceUserId;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Model that represents the User as exists in P&I database domain.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@ValidProvenanceUserId
@PiEmailConditionalValidation
@EntityListeners(AuditingEntityListener.class)
public class PiUser {

    /**
     * The ID of the user as they exist in P&I.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    @Type(type = "org.hibernate.type.PostgresUUIDType")
    private UUID userId;

    /**
     * The Sign in entry system the user was added with. (CFT IDAM, Crime IDAM, P&I AAD).
     */
    @Enumerated(EnumType.STRING)
    private UserProvenances userProvenance;

    /**
     * The user id of the user as per their provenance system.
     */
    @NotNull(message = "provenance user id must not be null")
    @NotBlank(message = "provenance user id must not be blank")
    private String provenanceUserId;

    /**
     * Email of the user. Validated at the class level by PiEmailConditionalValidation interface.
     */
    private String email;

    /**
     * Role of the user, Verified, Internal or Technical.
     */
    @Enumerated(EnumType.STRING)
    private Roles roles;

    /**
     * The timestamp of when the user was created.
     */
    @ApiModelProperty(hidden = true)
    private LocalDateTime createdDate;

    /**
     * The timestamp of when the user was last verified.
     */
    private LocalDateTime lastVerifiedDate;

    /**
     * The timestamp when the user was last signed in.
     */
    private LocalDateTime lastSignedInDate;
}
