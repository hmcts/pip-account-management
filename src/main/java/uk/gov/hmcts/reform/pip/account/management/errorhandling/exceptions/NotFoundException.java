package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Custom exception which handles not found errors.
 */
public class NotFoundException extends RuntimeException {

    private static final long serialVersionUID = -5622817523799460884L;

    /**
     * Constructor for custom NotFoundException.
     * @param message error message.
     */
    public NotFoundException(String message) {
        super(message);
    }
}
