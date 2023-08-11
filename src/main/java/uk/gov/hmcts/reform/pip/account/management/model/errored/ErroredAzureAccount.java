package uk.gov.hmcts.reform.pip.account.management.model.errored;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.account.management.model.AzureAccount;

import java.util.List;

/**
 * Class which represents a AzureAccount that has failed to be created.
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ErroredAzureAccount extends AzureAccount {

    /**
     * This is the error messages for why the account has failed to be created.
     */
    private List<String> errorMessages;

    /**
     * Constructor that takes in an existing account and converts it to an errored account.
     * @param azureAccount The account to be converted to an errored account.
     */
    public ErroredAzureAccount(AzureAccount azureAccount) {
        super(
            azureAccount.getAzureAccountId(),
            azureAccount.getEmail(),
            azureAccount.getPassword(),
            azureAccount.getFirstName(),
            azureAccount.getSurname(),
            azureAccount.getRole(),
            azureAccount.getDisplayName());
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && obj.getClass() == this.getClass();
    }

    @Override
    public int hashCode() {
        return 0;
    }
}
