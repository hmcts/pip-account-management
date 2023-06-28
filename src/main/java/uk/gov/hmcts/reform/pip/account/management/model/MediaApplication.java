package uk.gov.hmcts.reform.pip.account.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;


/**
 * Model that represents a media application for AAD access.
 */
@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class MediaApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    private UUID id;

    private String fullName;

    private String email;

    private String employer;

    private String image;

    private String imageName;

    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    private MediaApplicationStatus status;

    private LocalDateTime statusDate;
}
