package tn.esprit.users_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() == null ? "Unexpected error" : ex.getMessage();
        return ResponseEntity.status(resolveStatus(message)).body(Map.of("message", message));
    }

    private HttpStatus resolveStatus(String message) {
        String lower = message.toLowerCase();
        if (lower.contains("already in use")) {
            return HttpStatus.CONFLICT;
        }
        if (lower.contains("invalid credentials") || lower.contains("invalid role")) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (lower.contains("not found")) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
