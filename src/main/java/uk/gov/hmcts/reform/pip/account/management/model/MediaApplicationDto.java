package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValueOfEnum;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * DTO that represents a media application for AAD access.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaApplicationDto {

    @NotBlank(message = "fullName shouldn't be blank or null")
    private String fullName;

    @Email
    @NotBlank(message = "email shouldn't be blank or null")
    private String email;

    @NotBlank(message = "employer shouldn't be blank or null")
    private String employer;

    @NotNull(message = "status shouldn't be null")
    @ValueOfEnum(
        enumClass = MediaApplicationStatus.class,
        message = "status should be one of PENDING, REJECTED or APPROVED"
    )
    private String status;

    public MediaApplication toEntity() {
        MediaApplication entity = new MediaApplication();
        entity.setFullName(this.fullName);
        entity.setEmail(this.email);
        entity.setEmployer(this.employer);
        entity.setStatus(MediaApplicationStatus.valueOf(this.status));

        return entity;
    }
}
