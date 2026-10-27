package uk.gov.hmcts.reform.pip.account.management.model;

import io.hypersistence.utils.hibernate.type.array.ListArrayType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table
public class UserComm {
    private static final String DEFINITION = "text[]";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", insertable = false, updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    private CommType commType;

    private String message;

    @Type(ListArrayType.class)
    @Column(name = "user_provenances", columnDefinition = "text[]")
    private List<String> userProvenances;

    @Type(ListArrayType.class)
    @Column(name = "user_roles", columnDefinition = "text[]")
    private List<String> userRoles;

    @Type(ListArrayType.class)
    @Column(name = "location_ids", columnDefinition = "integer[]")
    private List<Integer> locationIds;
}
