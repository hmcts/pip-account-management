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
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.NotFoundException;
import uk.gov.hmcts.reform.pip.account.management.errorhandling.exceptions.SubscriptionNotFoundException;
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

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ExceptionResponse> handle(NotFoundException ex) {
        log.error(writeLog("404, Unable to find requested account / application / audit"));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(UserWithProvenanceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handle(UserWithProvenanceNotFoundException ex) {
        log.info(writeLog("404, " + ex.getMessage()));

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(generateExceptionResponse(ex.getMessage()));
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handle(SubscriptionNotFoundException ex) {

        log.error(writeLog(
            "404, Subscription has not been found. Cause: " + ex.getMessage()));

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

    //    /**
    //     * This exception creates the following style of message:
    //     * "Bad Request: userId must not be null, searchValue must not be blank" etc.
    //     *
    //     * @param ex - a MethodArgumentNotValidException, created when an invalid json object is passed as a
    //     *            parameter to
    //     *           the post endpoint (e.g. an empty request)
    //     * @return - a ResponseEntity containing the exception response
    //     */
    //    @ExceptionHandler(MethodArgumentNotValidException.class)
    //    public ResponseEntity<ExceptionResponse> handle(MethodArgumentNotValidException ex) {
    //
    //        ExceptionResponse exceptionResponse = new ExceptionResponse();
    //        StringBuilder responseText = new StringBuilder("Bad Request: ");
    //        for (int i = 0; i < ex.getBindingResult().getErrorCount(); i++) {
    //            responseText.append(ex.getFieldErrors().get(i).getField())
    //                .append(' ')
    //                .append(ex.getBindingResult().getAllErrors().get(i).getDefaultMessage())
    //                .append(", ");
    //        }
    //        exceptionResponse.setMessage(responseText.substring(0, responseText.length() - 2));
    //        exceptionResponse.setTimestamp(LocalDateTime.now());
    //
    //        log.error(writeLog(
    //            "400, Invalid argument provided when creating subscriptions. Cause: "
    //                + exceptionResponse.getMessage()));
    //
    //        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //            .body(exceptionResponse);
    //    }
    //
    //    /**
    //     * This will create a message of the following style:
    //     * "Bad Request: SearchType {search} should be one of the following types: [ LOCATION_ID CASE_ID CASE_URN ]"
    //     * Note: this exception also covers the "channel" enum in the same way.
    //     *
    //     * @param ex - an invalidformatexception, created when e.g. a value that does not adhere to an enum
    //     *             restriction
    //     * @return - a ResponseEntity containing the exception response
    //     */
    //    @ExceptionHandler(InvalidFormatException.class)
    //    public ResponseEntity<ExceptionResponse> handle(InvalidFormatException ex) {
    //        StringBuilder responseText = new StringBuilder(100);
    //        responseText.append("Bad Request: ").append(ex.getTargetType().getSimpleName()).append(' ')
    //            .append(ex.getValue()).append(" should be one of the following types: [ ");
    //        for (int i = 0; i < ex.getTargetType().getFields().length; i++) {
    //            responseText.append(ex.getTargetType().getFields()[i].getName()).append(' ');
    //        }
    //        responseText.append(']');
    //        ExceptionResponse exceptionResponse = new ExceptionResponse();
    //        exceptionResponse.setMessage(responseText.toString());
    //        exceptionResponse.setTimestamp(LocalDateTime.now());
    //
    //        log.error(writeLog(
    //            "400, Invalid argument provided when creating subscriptions. Cause: " + ex.getMessage()));
    //
    //        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
    //            .body(exceptionResponse);
    //    }

    private ExceptionResponse generateExceptionResponse(String message) {
        ExceptionResponse exceptionResponse = new ExceptionResponse();
        exceptionResponse.setMessage(message);
        exceptionResponse.setTimestamp(LocalDateTime.now());
        return exceptionResponse;
    }
}
