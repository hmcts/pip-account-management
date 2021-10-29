package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Custom exception which handles errors when communicating with Azure.
 */
public class AzureCustomException extends Exception {

    /**
     * Custom exception which handles Azure messages.
     * @param message The message that describes the exception.
     */
    public AzureCustomException(String message) {
        super(message);
    }

}
