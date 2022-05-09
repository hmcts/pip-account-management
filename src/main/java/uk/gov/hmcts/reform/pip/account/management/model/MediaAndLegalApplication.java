package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;


/**
 * Model that represents a media or orphaned legal professional application for AAD access.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaAndLegalApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    @Type(type = "org.hibernate.type.PostgresUUIDType")
    private UUID id;

    private String fullName;

    private String email;

    private String employer;

    private String image;

    private String imageName;

    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    private MediaLegalApplicationStatus status;

    private LocalDateTime statusDate;
}
