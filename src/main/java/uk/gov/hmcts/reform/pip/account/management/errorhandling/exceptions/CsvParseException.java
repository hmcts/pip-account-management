package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * This exception class handles any issues when parsing the CSV file.
 */
public class CsvParseException extends RuntimeException {

    private static final long serialVersionUID = 1437013721590876557L;

    public CsvParseException(String message) {
        super("Failed to parse CSV File due to: " + message);
    }
}
