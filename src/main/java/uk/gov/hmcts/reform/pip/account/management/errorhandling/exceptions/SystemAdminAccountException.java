package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

import lombok.Getter;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSystemAdminAccount;

/**
 * Exception class which handles an exception when creating the System Admin account.
 */
@Getter
public class SystemAdminAccountException extends RuntimeException {

    private static final long serialVersionUID = 5618103537116781009L;

    private final transient ErroredSystemAdminAccount erroredSystemAdminAccount;


    public SystemAdminAccountException(ErroredSystemAdminAccount erroredSystemAdminAccount) {

        super();
        this.erroredSystemAdminAccount = erroredSystemAdminAccount;

    }


}
