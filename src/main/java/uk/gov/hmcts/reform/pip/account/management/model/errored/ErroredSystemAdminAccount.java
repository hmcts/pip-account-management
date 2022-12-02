package uk.gov.hmcts.reform.pip.account.management.model.errored;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uk.gov.hmcts.reform.pip.account.management.model.SystemAdminAccount;

import java.util.List;

/**
 * A class which represents an errored system admin user.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ErroredSystemAdminAccount extends SystemAdminAccount {

    /**
     * This is the error messages for why the system admin account has failed to create.
     */
    private List<String> errorMessages;

    /**
     * This flag indicates whether the user is above the maximum number of system admin accounts that are allowed.
     */
    private boolean isAboveMaxSystemAdmin;

    /**
     * This flag says whether the user already existed. This allows the frontend to parse
     * why the user failed to create, and in turn display an appropriate error message
     */
    private boolean isDuplicate;

    public ErroredSystemAdminAccount(SystemAdminAccount systemAdminAccount) {
        super(systemAdminAccount.getEmail(), systemAdminAccount.getFirstName(), systemAdminAccount.getSurname());
    }

}
