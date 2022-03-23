package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Exception for a user not having correct classification level for target.
 */
public class ForbiddenPermissionsException extends RuntimeException {

    private static final long serialVersionUID = 869436819230575103L;

    /**
     * Constructor for ForbiddenPermissionsException.
     *
     * @param message Error message
     */
    public ForbiddenPermissionsException(String message) {
        super(message);
    }
}
