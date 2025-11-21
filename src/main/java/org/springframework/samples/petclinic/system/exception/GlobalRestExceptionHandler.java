package org.springframework.samples.petclinic.system.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;

/**
 * Global exception handler for REST controllers, producing structured validation error responses.
 */
@RestControllerAdvice(annotations = org.springframework.web.bind.annotation.RestController.class)
public class GlobalRestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                               HttpServletRequest request) {
        HttpStatus status = HttpStatus.BAD_REQUEST;

        ValidationErrorResponse body = new ValidationErrorResponse();
        body.setTimestamp(OffsetDateTime.now());
        body.setStatus(status.value());
        body.setError(status.getReasonPhrase());
        body.setMessage("Validation failed");
        body.setPath(request != null ? request.getRequestURI() : null);

        // Field errors
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            Object rejected = fe.getRejectedValue();
            // Avoid serializing very large objects by using toString; Jackson will handle primitives gracefully
            body.addError(new ValidationError(fe.getObjectName(), fe.getField(), rejected, fe.getDefaultMessage()));
        }

        // Global (object-level) errors
        for (ObjectError ge : ex.getBindingResult().getGlobalErrors()) {
            body.addError(new ValidationError(ge.getObjectName(), null, null, ge.getDefaultMessage()));
        }

        return ResponseEntity.status(status).body(body);
    }
}
