package uk.gov.hmcts.reform.pip.account.management.errorhandling;

import com.fasterxml.jackson.databind.JsonMappingException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.CsvParseException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.ForbiddenRoleUpdateException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SystemAdminAccountException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.UserWithProvenanceNotFoundException;
import uk.gov.hmcts.reform.pip.account.management.model.errored.ErroredSystemAdminAccount;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static uk.gov.hmcts.reform.pip.model.LogBuilder.writeLog;

/**
 * Global exception handler, that captures exceptions thrown by the controllers, and encapsulates
 * the logic to handle them and return a standardised response to the user.
 */
@Slf4j
@ControllerAdvice
@SuppressWarnings({"PMD.TooManyMethods"})
public class GlobalExceptionHandler {

    /**
     * Exception handler that handles Invalid Json exceptions
     * and returns a 400 bad request error code.
     * @param ex The exception that has been thrown.
     * @return The error response, modelled using the ExceptionResponse object.
     */
    @ExceptionHandler(JsonMappingException.class)
    public ResponseEntity<ExceptionResponse> handle(
        JsonMappingException ex) {
        log.error(writeLog("400, Unable to create account from provided JSON"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ExceptionResponse> handle(MissingRequestHeaderException ex) {
        log.error(writeLog("400, Missing headers from request"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ExceptionResponse> handle(ConstraintViolationException ex) {
        log.error(writeLog("400, Error while validating the JSON provided"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(ForbiddenRoleUpdateException.class)
    public ResponseEntity<ExceptionResponse> handle(ForbiddenRoleUpdateException ex) {
        log.error(writeLog("403, " + ex.getMessage()));

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ExceptionResponse> handle(NotFoundException ex) {
        log.error(writeLog("404, Unable to find requested account / application"));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(UserWithProvenanceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handle(UserWithProvenanceNotFoundException ex) {
        log.info(writeLog("404, " + ex.getMessage()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(CsvParseException.class)
    public ResponseEntity<ExceptionResponse> handle(CsvParseException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ExceptionResponse> handle(IllegalArgumentException ex) {
        log.error(writeLog("400, Illegal input parameter"));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(SystemAdminAccountException.class)
    public ResponseEntity<ErroredSystemAdminAccount> handle(SystemAdminAccountException ex) {
        log.error(writeLog("400, Error while creating a system admin account"));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getErroredSystemAdminAccount());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handle(BindException ex) {
        Map<String, String> errorMap = new ConcurrentHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errorMap.put(error.getField(),
                                                                             error.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorMap);
    }

    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        exceptionResponse.setTimestamp(LocalDateTime.now());
        return exceptionResponse;
    }
}
