package uk.gov.hmcts.reform.demo.errorhandling.exceptions;

/**
 * Exception that captures the message when an account is not found.
 */
public class AccountNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -894013046243942471L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public AccountNotFoundException(String message) {
        super(message);
    }

}
