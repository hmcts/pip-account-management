package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidEmail;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * Model that represents the User as exists in P&I database domain
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
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
    @NotNull
    @NotBlank
    private String provenanceUserId;

    /**
     * Email of the user.
     */
    @ValidEmail
    private String email;

    /**
     * Role of the user, Verified, Internal or Technical.
     */
    @Enumerated(EnumType.STRING)
    private Roles roles;
}
