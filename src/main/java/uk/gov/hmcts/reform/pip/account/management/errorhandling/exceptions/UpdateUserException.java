package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;

import java.io.Serial;

/**
 * Exception thrown when user tries to update a user with an invalid role.
 */
public class UpdateUserException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 8342774683103699420L;

    public UpdateUserException(PiUser piUser) {
        super(String.format("Invalid role %s for user with provenance %s",
                            piUser.getRoles(), piUser.getUserProvenance()));
    }


}
