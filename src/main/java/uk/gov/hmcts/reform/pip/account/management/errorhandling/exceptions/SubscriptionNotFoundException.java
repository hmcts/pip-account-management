package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Exception that captures the message when a subscription is not found.
 */
public class SubscriptionNotFoundException extends RuntimeException {

    private static final long serialVersionUID = -7362402549762812559L;

    public SubscriptionNotFoundException(String message) {
        super(message);
    }
}
