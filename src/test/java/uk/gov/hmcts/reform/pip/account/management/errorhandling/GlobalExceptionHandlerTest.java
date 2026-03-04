package uk.gov.hmcts.reform.pip.account.management.errorhandling;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import uk.gov.hmcts.reform.pip.account.management.controllers.account.AccountController;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SubscriptionNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SystemAdminAccountException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.ThirdPartyHealthCheckException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UpdateUserException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserWithProvenanceNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.account.PiUser;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSystemAdminAccount;
import uk.gov.hmcts.reform.pip.model.account.Roles;
import uk.gov.hmcts.reform.pip.model.account.UserProvenances;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final String ERROR_MESSAGE = "Exception Message";
    private static final String NOT_NULL_MESSAGE = "Exception body should not be null";
    private static final String EXCEPTION_BODY_NOT_MATCH = "Exception body doesn't match test message";
    public static final String RESPONSE_SHOULD_CONTAIN_A_BODY = "Response should contain a body";

    private static final String SHOULD_BE_BAD_REQUEST_EXCEPTION = "Should be bad request exception";
    private static final String SHOULD_BE_500_EXCEPTION = "Should be internal server error exception";

    @Mock
    InvalidFormatException invalidFormatException;

    @Mock
    MethodArgumentNotValidException methodArgumentNotValidException;

    @Mock
    BindingResult bindingResult;

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
                "createUsers", UUID.class, List.class), 0));
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(missingRequestHeaderException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), "Status code should be bad request");
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_A_BODY);
        assertEquals("Required request header 'test' for method parameter type UUID is not present",
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
    void testUserWithProvenanceNotFoundException() {
        UserWithProvenanceNotFoundException notFoundException = new UserWithProvenanceNotFoundException("123");
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(notFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), RESPONSE_SHOULD_CONTAIN_A_BODY);
        assertEquals("No user found with provenance user ID: 123",
                     responseEntity.getBody().getMessage(), EXCEPTION_BODY_NOT_MATCH);
    }

    @Test
    void testHandleSubscriptionNotFoundMethod() {
        SubscriptionNotFoundException subscriptionNotFoundException
            = new SubscriptionNotFoundException(ERROR_MESSAGE);

        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(subscriptionNotFoundException);

        assertEquals(HttpStatus.NOT_FOUND, responseEntity.getStatusCode(), "Status code should be not found");
        assertNotNull(responseEntity.getBody(), "Response should contain a body");
        assertEquals(ERROR_MESSAGE, responseEntity.getBody().getMessage(),
                     "The message should match the message passed in");
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
    void testUpdateUserException() {
        PiUser piUser = new PiUser();
        piUser.setRoles(Roles.INTERNAL_ADMIN_CTSC);
        piUser.setUserProvenance(UserProvenances.PI_AAD);

        UpdateUserException updateUserException = new UpdateUserException(piUser);
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(updateUserException);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode(), SHOULD_BE_BAD_REQUEST_EXCEPTION);
        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
        assertTrue(responseEntity.getBody().getMessage().contains("Invalid role INTERNAL_ADMIN_CTSC "
                                                                      + "for user with provenance PI_AAD"),
                   EXCEPTION_BODY_NOT_MATCH);
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

    @Test
    void testThirdPartyHealthCheckException() {
        ThirdPartyHealthCheckException exception = new ThirdPartyHealthCheckException(ERROR_MESSAGE);
        ResponseEntity<ExceptionResponse> responseEntity = globalExceptionHandler.handle(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode(), SHOULD_BE_500_EXCEPTION);
        assertNotNull(responseEntity.getBody(), NOT_NULL_MESSAGE);
        assertTrue(responseEntity.getBody().getMessage().contains(ERROR_MESSAGE), EXCEPTION_BODY_NOT_MATCH);
    }
}
