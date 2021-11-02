package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Custom exception which handles errors when communicating with Azure.
 */
public class AzureCustomException extends Exception {

    private static final long serialVersionUID = 381344495963342091L;

    /**
     * Custom exception which handles Azure messages.
     * @param message The message that describes the exception.
     */
    public AzureCustomException(String message) {
        super(message);
    }

}
