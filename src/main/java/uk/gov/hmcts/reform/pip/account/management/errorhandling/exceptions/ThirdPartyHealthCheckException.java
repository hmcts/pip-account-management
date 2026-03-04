package uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions;

public class ThirdPartyHealthCheckException extends RuntimeException {
    private static final long serialVersionUID = 982353173933446694L;

    public ThirdPartyHealthCheckException(String message) {
        super(message);
    }
}
