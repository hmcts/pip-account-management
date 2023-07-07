package uk.gov.hmcts.reform.pip.account.management.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.PiEmailConditionalValidation;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidProvenanceUserId;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.time.LocalDateTime;
import java.util.UUID;

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
     * The forenames of the user.
     */
    private String forenames;

    /**
     * The surnames of the user.
     */
    private String surname;

    /**
     * The timestamp of when the user was created.
     */
    @Schema(hidden = true)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime createdDate;

    /**
     * The timestamp of when the user was last verified.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastVerifiedDate;

    /**
     * The timestamp when the user was last signed in.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")
    private LocalDateTime lastSignedInDate;
}
