package uk.gov.hmcts.reform.pip.account.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "userId shouldn't be blank or null")
    private String userId;

    @Email
    @NotBlank(message = "email shouldn't be blank or null")
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "roles shouldn't be null")
    private Roles roles;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "user provenance shouldn't be null")
    private UserProvenances userProvenance;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "action shouldn't be null")
    private AuditAction action;

    @NotNull(message = "details must be provided")
    @Size(min = 1, max = 255, message = "details should be between 1 and 255 characters")
    private String details;

    @CreatedDate
    private LocalDateTime timestamp;

    public AuditLog(String userId, String userEmail, Roles roles, UserProvenances userProvenance,
                    AuditAction action, String details) {
        this.userId = userId;
        this.userEmail = userEmail;
        this.roles = roles;
        this.userProvenance = userProvenance;
        this.action = action;
        this.details = details;
    }

}
