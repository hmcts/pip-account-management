package uk.gov.hmcts.reform.pip.account.management.model;

import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class CftUser {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    @ApiModelProperty(hidden = true)
    @Type(type = "org.hibernate.type.PostgresUUIDType")
    private UUID id;

    private UUID idamUid;

    private String username;

    private String name;

    private String givenName;

    private String familyName;

    private String roleCategory;

    private LocalDateTime last_login;

    private String idam;
}
