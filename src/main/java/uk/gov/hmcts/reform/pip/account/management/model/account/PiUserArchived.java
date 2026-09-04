package uk.gov.hmcts.reform.pip.account.management.model.account;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PiUserArchived {
    @Id
    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    private UserProvenances userProvenance;

    private String provenanceUserId;

    private String email;

    @Enumerated(EnumType.STRING)
    private Roles roles;

    private LocalDateTime lastSignedInDate;

    private LocalDateTime archivedDate;

    public PiUserArchived(PiUser piUser) {
        this.setUserId(piUser.getUserId());
        this.setProvenanceUserId(piUser.getProvenanceUserId());
        this.setUserProvenance(piUser.getUserProvenance());
        this.setEmail(piUser.getEmail());
        this.setRoles(piUser.getRoles());
        this.setLastSignedInDate(piUser.getLastSignedInDate());
        this.setArchivedDate(LocalDateTime.now());
    }
}
