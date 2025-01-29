package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Exception that captures the message when a subscription is not found.
 */
public class SubscriptionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -7362402549762812559L;

    /**
     * Constructor for the Exception.
     * @param message The message to return to the end user
     */
    public SubscriptionNotFoundException(String message) {
        super(message);
    }

}
