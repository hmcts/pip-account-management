package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that represents a media application for AAD access.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaApplicationDto {
    private String fullName;

    private String email;

    private String employer;

    private MediaApplicationStatus status;

    public MediaApplication toEntity() {
        MediaApplication entity = new MediaApplication();
        entity.setFullName(this.fullName);
        entity.setEmail(this.email);
        entity.setEmployer(this.employer);
        entity.setStatus(this.status);

        return entity;
    }
}
