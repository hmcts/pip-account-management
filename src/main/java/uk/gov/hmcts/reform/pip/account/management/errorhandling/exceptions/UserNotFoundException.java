package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Exception for a user not being found.
 */
public class UserNotFoundException extends NotFoundException {

    private static final long serialVersionUID = -8379848393786710695L;

    private static final String MESSAGE = "No user found with the %s: %s";

    /**
     * Constructor for custom UserNotFoundException.
     *
     * @param idType the type of id searching, whether userId or provenanceUserId.
     * @param id the id searched for.
     */
    public UserNotFoundException(String idType, String id) {
        super(String.format(MESSAGE, idType, id));
    }
}
