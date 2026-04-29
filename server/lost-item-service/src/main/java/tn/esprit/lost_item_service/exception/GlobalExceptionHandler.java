package tn.esprit.lost_item_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String TIMESTAMP_KEY = "timestamp";
    private static final String STATUS_KEY = "status";
    private static final String MESSAGE_KEY = "message";
    private static final String ERROR_CODE_KEY = "errorCode";
    private static final String ERRORS_KEY = "errors";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP_KEY, LocalDateTime.now().toString());
        response.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
        response.put(MESSAGE_KEY, "Validation error");
        response.put(ERRORS_KEY, fieldErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateReportException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateReport(DuplicateReportException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP_KEY, LocalDateTime.now().toString());
        response.put(STATUS_KEY, HttpStatus.CONFLICT.value());
        response.put(MESSAGE_KEY, ex.getMessage());
        response.put(ERROR_CODE_KEY, "DUPLICATE_REPORT");

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP_KEY, LocalDateTime.now().toString());
        response.put(STATUS_KEY, HttpStatus.FORBIDDEN.value());
        response.put(MESSAGE_KEY, ex.getMessage());
        response.put(ERROR_CODE_KEY, "ACCESS_DENIED");

        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put(TIMESTAMP_KEY, LocalDateTime.now().toString());
        response.put(STATUS_KEY, HttpStatus.BAD_REQUEST.value());
        response.put(MESSAGE_KEY, ex.getMessage());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
