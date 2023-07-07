package uk.gov.hmcts.reform.pip.account.management.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValueOfEnum;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;
import uk.gov.hmcts.reform.pip.model.enums.AuditAction;

/**
 * DTO that represents an audit log entry sent in from the frontend.
 */
@Data
@AllArgsConstructor
public class AuditLogDto {

    @NotBlank(message = "userId shouldn't be blank or null")
    private String userId;

    @Email
    @NotBlank(message = "email shouldn't be blank or null")
    private String userEmail;

    @NotNull(message = "roles shouldn't be null")
    @ValueOfEnum(
        enumClass = Roles.class,
        message = "roles value should be contained in the enum"
    )
    private String roles;

    @NotNull(message = "user provenance shouldn't be null")
    @ValueOfEnum(
        enumClass = UserProvenances.class,
        message = "user provenance value should be contained in the enum"
    )
    private String userProvenance;

    @NotNull(message = "action shouldn't be null")
    @ValueOfEnum(
        enumClass = AuditAction.class,
        message = "action value should be contained in the enum"
    )
    private String action;

    @NotBlank(message = "details shouldn't be blank or null")
    private String details;

    public AuditLog toEntity() {
        AuditLog entity = new AuditLog();
        entity.setUserId(this.userId);
        entity.setUserEmail(this.userEmail);
        entity.setRoles(Roles.valueOf(this.roles));
        entity.setUserProvenance(UserProvenances.valueOf(this.userProvenance));
        entity.setAction(AuditAction.valueOf(this.action));
        entity.setDetails(this.details);

        return entity;
    }
}
