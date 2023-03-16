package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Exception for a user not having permission to update a users role.
 */
public class ForbiddenRoleUpdateException extends RuntimeException {

    private static final long serialVersionUID = 869436819230575103L;

    /**
     * Constructor for ForbiddenRoleUpdateException.
     *
     * @param message Error message
     */
    public ForbiddenRoleUpdateException(String message) {
        super(message);
    }
}
