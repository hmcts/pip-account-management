package uk.gov.hmcts.reform.pip.account.management.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Model which represents a system admin account request.
 */
@NoArgsConstructor
@Getter
@Setter
public class SystemAdminAccount {

    @NotEmpty
    @NotNull
    private String email;

    @NotEmpty
    @NotNull
    private String firstName;

    @NotEmpty
    @NotNull
    private String surname;

    public SystemAdminAccount(String email, String firstName, String surname) {
        this.firstName = firstName;
        this.surname = surname;
        this.email = email;
    }

    public AzureAccount convertToAzureAccount() {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(this.getEmail());
        azureAccount.setFirstName(firstName);
        azureAccount.setSurname(surname);
        azureAccount.setRole(Roles.SYSTEM_ADMIN);
        return azureAccount;
    }

    public PiUser convertToPiUser(String provenanceUserId) {
        LocalDateTime localDateTime = LocalDateTime.now(ZoneId.of("UTC"));
        PiUser piUser = new PiUser();
        piUser.setEmail(this.getEmail());
        piUser.setRoles(Roles.SYSTEM_ADMIN);
        piUser.setUserProvenance(UserProvenances.PI_AAD);
        piUser.setProvenanceUserId(provenanceUserId);
        piUser.setLastVerifiedDate(localDateTime);
        piUser.setLastSignedInDate(localDateTime);
        piUser.setCreatedDate(localDateTime);
        return piUser;
    }

}
