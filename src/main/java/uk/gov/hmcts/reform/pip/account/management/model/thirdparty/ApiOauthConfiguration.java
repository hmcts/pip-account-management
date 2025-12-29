package uk.gov.hmcts.reform.pip.account.management.model.thirdparty;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApiOauthConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    private UUID id;

    @NotNull
    private UUID userId;

    @NotNull
    @NotBlank
    private String destinationUrl;

    @NotNull
    @NotBlank
    private String tokenUrl;

    @NotNull
    @NotBlank
    private String clientIdKey;

    @NotNull
    @NotBlank
    private String clientSecretKey;

    @NotNull
    @NotBlank
    private String scopeKey;

    @Schema(hidden = true)
    @CreatedDate
    private LocalDateTime createdDate;

    @Schema(hidden = true)
    @LastModifiedDate
    private LocalDateTime lastUpdatedDate;
}
