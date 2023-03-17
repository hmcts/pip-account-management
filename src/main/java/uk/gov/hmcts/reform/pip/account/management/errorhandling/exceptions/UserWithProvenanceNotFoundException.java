package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

public class UserWithProvenanceNotFoundException extends RuntimeException  {
    private static final long serialVersionUID = 2605566330198759035L;

    private static final String MESSAGE = "No user found with provenance user ID: %s";

    /**
     * Constructor for custom NotFoundException.
     * @param id the provenance id searched for.
     */
    public UserWithProvenanceNotFoundException(String id) {
        super(String.format(MESSAGE, id));
    }
}
