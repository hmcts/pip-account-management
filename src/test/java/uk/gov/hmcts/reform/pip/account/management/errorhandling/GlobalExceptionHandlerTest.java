package uk.gov.hmcts.reform.pip.account.management.errorhandling;

import com.fasterxml.jackson.databind.JsonMappingException;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import uk.gov.hmcts.reform.pip.account.management.controllers.AccountController;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;

import java.util.List;
import javax.validation.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GlobalExceptionHandlerTest {

    private static final String ERROR_MESSAGE = "Exception Message";
    private static final String NOT_NULL_MESSAGE = "Exception body should not be null";
    private static final String EXCEPTION_BODY_NOT_MATCH = "Exception body doesn't match test message";

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void testJsonMappingException() {

        JsonMappingException jsonMappingException = new JsonMappingException(null, ERROR_MESSAGE);
        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handle(jsonMappingException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be bad request");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
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
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("Required request header 'test' for method parameter type String is not present",
                     responseEntity.getBody().getMessage(), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testConstrainViolationException() {
        ConstraintViolationException constraintViolationException =
            new ConstraintViolationException(ERROR_MESSAGE, null);

        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(constraintViolationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(),
                     "Should be bad request exception");
        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
        assertTrue(responseEntity.getBody().getMessage().contains(ERROR_MESSAGE), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testNotFoundException() {
        NotFoundException notFoundException = new NotFoundException(ERROR_MESSAGE);
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(notFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals(ERROR_MESSAGE,
                     responseEntity.getBody().getMessage(), EXCEPTION_BODY_NOT_MATCH);
    }

}
