package uk.gov.hmcts.reform.pip.account.management.errorhandling;

import org.hibernate.validator.internal.engine.ConstraintViolationImpl;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    @Test
    void testConstraintViolationTest() {

        GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

        ConstraintViolation<? extends GlobalExceptionHandlerTest> constraintViolation =
            ConstraintViolationImpl.forBeanValidation(
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
            globalExceptionHandler.handle(constraintViolationException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be bad request");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals("path: This is an error", responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
    }
}
