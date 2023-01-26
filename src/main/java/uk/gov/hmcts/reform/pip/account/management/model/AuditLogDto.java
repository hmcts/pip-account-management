package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValueOfEnum;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

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
        entity.setAction(AuditAction.valueOf(this.action));
        entity.setDetails(this.details);

        return entity;
    }
}
