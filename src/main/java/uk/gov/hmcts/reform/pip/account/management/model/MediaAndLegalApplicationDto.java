package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO that represents a media or orphaned legal professional application for AAD access.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaAndLegalApplicationDto {
    private String fullName;

    private String email;

    private String employer;

    private MediaLegalApplicationStatus status;

    public MediaAndLegalApplication toEntity() {
        MediaAndLegalApplication entity = new MediaAndLegalApplication();
        entity.setFullName(this.fullName);
        entity.setEmail(this.email);
        entity.setEmployer(this.employer);
        entity.setStatus(this.status);

        return entity;
    }
}
