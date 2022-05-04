package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Exception for handling files that could retrieve input streams.
 */
public class FileException extends RuntimeException {

    private static final long serialVersionUID = 8574604777519490260L;

    /**
     * Constructor for the exception.
     */
    public FileException(String message) {
        super(message);
    }
}
