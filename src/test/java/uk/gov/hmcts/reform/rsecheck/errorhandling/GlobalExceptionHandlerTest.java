package uk.gov.hmcts.reform.rsecheck.errorhandling;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.demo.errorhandling.ExceptionResponse;
import uk.gov.hmcts.reform.demo.errorhandling.GlobalExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class GlobalExceptionHandlerTest {

    @Test
    public void testConstraintViolationTest() {

        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        ConstraintViolation<? extends GlobalExceptionHandlerTest> constraintViolation = ConstraintViolationImpl.forBeanValidation(
            "template",
            null,
            null,
            "This is an error",
            this.getClass(),
            null,
            null,
            null,
            PathImpl.createPathFromString("path"),
            null,
            null
        );

        Set<ConstraintViolation<?>> constraintViolations = Set.of(constraintViolation);

        ConstraintViolationException constraintViolationException
            = new ConstraintViolationException(constraintViolations);

        ResponseEntity<ExceptionResponse> responseEntity =
            globalExceptionHandler.handleMethodValidationError(constraintViolationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be bad request");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("path: This is an error", responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
    }
}
