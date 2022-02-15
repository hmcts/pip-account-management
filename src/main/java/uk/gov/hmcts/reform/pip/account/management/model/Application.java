package uk.gov.hmcts.reform.pip.account.management.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.hibernate.annotations.Type;
import uk.gov.hmcts.reform.pip.account.management.validation.annotations.ValidEmail;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

/**
 * Model which represents the list of hearings for a court.
 */
@Entity
@Data
public class Application {

    /**
     * The ID of the application.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    @Type(type = "org.hibernate.type.PostgresUUIDType")
    private UUID applicationId;

    /**
     * The email address for the application.
     */
    @ValidEmail
    private String email;

    /**
     * The user name of the applicant.
     */
    private String userName;

    /**
     * The employer of the applicant.
     */
    private String employer;

    /**
     * The date of the request.
     */
    @JsonProperty
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date requestDate;

    /**
     * The status of the application
     */
    private ApplicationStatus status;

    /**
     * The date of the status.
     */
    @JsonProperty
    @JsonFormat(pattern = "dd/MM/yyyy HH:mm:ss")
    private Date statusDate;

}

