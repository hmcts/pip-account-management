package uk.gov.hmcts.reform.pip.account.management.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Model which represents a system admin account request
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SystemAdminAccount extends PiUser {

    @NotEmpty
    @NotNull
    private String firstName;

    @NotEmpty
    @NotNull
    private String surname;

    public SystemAdminAccount(String email, String firstName, String surname) {
        this.firstName = firstName;
        this.surname = surname;
        this.setEmail(email);
    }

    public AzureAccount convertToAzureAccount() {
        AzureAccount azureAccount = new AzureAccount();
        azureAccount.setEmail(this.getEmail());
        azureAccount.setFirstName(firstName);
        azureAccount.setSurname(surname);
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
