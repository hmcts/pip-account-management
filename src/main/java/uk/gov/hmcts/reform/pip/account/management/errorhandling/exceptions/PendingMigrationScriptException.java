package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

/**
 * Flyway exception class for migrations when they've not been applied.
 */
public class PendingMigrationScriptException extends RuntimeException {

    private static final long serialVersionUID = 2484917257349765563L;

    public PendingMigrationScriptException(String script) {
        super("Found migration not yet applied: " + script);
    }
}
