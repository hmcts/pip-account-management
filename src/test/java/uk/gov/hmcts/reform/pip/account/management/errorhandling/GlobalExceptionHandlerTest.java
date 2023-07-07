package uk.gov.hmcts.reform.pip.account.management.errorhandling;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import uk.gov.hmcts.reform.pip.account.management.controllers.AccountController;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.ForbiddenRoleUpdateException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SystemAdminAccountException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserWithProvenanceNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSystemAdminAccount;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private static final String ERROR_MESSAGE = "Exception Message";
    private static final String NOT_NULL_MESSAGE = "Exception body should not be null";
    private static final String EXCEPTION_BODY_NOT_MATCH = "Exception body doesn't match test message";
    public static final String RESPONSE_SHOULD_CONTAIN_A_BODY = "Response should contain a body";

    private static final String SHOULD_BE_BAD_REQUEST_EXCEPTION = "Should be bad request exception";

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void testJsonMappingException() {

        JsonMappingException jsonMappingException = new JsonMappingException(null, ERROR_MESSAGE);
        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(jsonMappingException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be bad request");
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_A_BODY);
        assertEquals(ERROR_MESSAGE, responseEntity.getBody().getMessage(),
                     EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testMissingRequestHeaderException() throws NoSuchMethodException {
        MissingRequestHeaderException missingRequestHeaderException =
            new MissingRequestHeaderException("test", MethodParameter.forExecutable(AccountController.class.getMethod(
                "createUsers", String.class, List.class), 0));
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(missingRequestHeaderException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be bad request");
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_A_BODY);
        assertEquals("Required request header 'test' for method parameter type String is not present",
                     responseEntity.getBody().getMessage(), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testConstrainViolationException() {
        ConstraintViolationException constraintViolationException =
            new ConstraintViolationException(ERROR_MESSAGE, null);

        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(constraintViolationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), SHOULD_BE_BAD_REQUEST_EXCEPTION);
        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
        assertTrue(responseEntity.getBody().getMessage().contains(ERROR_MESSAGE), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testNotFoundException() {
        NotFoundException notFoundException = new NotFoundException(ERROR_MESSAGE);
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(notFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_A_BODY);
        assertEquals(ERROR_MESSAGE,
                     responseEntity.getBody().getMessage(), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testForbiddenRoleUpdateException() {
        ForbiddenRoleUpdateException forbiddenRoleUpdateException = new ForbiddenRoleUpdateException(ERROR_MESSAGE);
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(forbiddenRoleUpdateException);

        assertEquals(HttpStatus.FORBIDDEN, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_A_BODY);
        assertEquals(ERROR_MESSAGE,
                     responseEntity.getBody().getMessage(), EXCEPTION_BODY_NOT_MATCH
        );
    }

    @Test
    void testUserWithProvenanceNotFoundException() {
        UserWithProvenanceNotFoundException notFoundException = new UserWithProvenanceNotFoundException("123");
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(notFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_A_BODY);
        assertEquals("No user found with provenance user ID: 123",
                     responseEntity.getBody().getMessage(), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testCsvParseException() {
        CsvParseException csvParseException = new CsvParseException(ERROR_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(csvParseException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), SHOULD_BE_BAD_REQUEST_EXCEPTION);
        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
        assertTrue(
            responseEntity.getBody().getMessage()
                .contains(ERROR_MESSAGE),
            EXCEPTION_BODY_NOT_MATCH
        );
    }

    @Test
    void testIllegalArgumentException() {
        IllegalArgumentException illegalArgumentException = new IllegalArgumentException(ERROR_MESSAGE, null);
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(illegalArgumentException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), SHOULD_BE_BAD_REQUEST_EXCEPTION);
        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
        assertTrue(responseEntity.getBody().getMessage().contains(ERROR_MESSAGE), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testSystemAdminException() {
        ErroredSystemAdminAccount erroredSystemAdminAccount = new ErroredSystemAdminAccount();
        erroredSystemAdminAccount.setFirstName("Test");
        erroredSystemAdminAccount.setErrorMessages(List.of("Error message A"));

        SystemAdminAccountException systemAdminAccountException =
            new SystemAdminAccountException(erroredSystemAdminAccount);

        ResponseEntity<ErroredSystemAdminAccount> responseEntity =
            globalExceptionHandler.handle(systemAdminAccountException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), SHOULD_BE_BAD_REQUEST_EXCEPTION);

        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
        assertEquals(responseEntity.getBody(), erroredSystemAdminAccount,
                     "Returned errored account should match");

    }

    @Test
    void testBindException() {
        BindException bindException = new BindException(ERROR_MESSAGE, null);
        ResponseEntity<Map<String, String>> responseEntity = globalExceptionHandler.handle(bindException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), SHOULD_BE_BAD_REQUEST_EXCEPTION);
        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
    }
}
