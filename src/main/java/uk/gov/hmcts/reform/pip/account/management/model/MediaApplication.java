package uk.gov.hmcts.reform.pip.account.management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    @NotBlank(message = "fullName shouldn't be blank or null")
    private String fullName;

    @Email
    @NotBlank(message = "email shouldn't be blank or null")
    private String email;

    @NotBlank(message = "employer shouldn't be blank or null")
    private String employer;

    private String image;

    private String imageName;

    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "status shouldn't be null")
    private MediaApplicationStatus status;

    private LocalDateTime statusDate;
}
