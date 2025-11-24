package org.springframework.samples.petclinic.system;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Global exception handler for REST controllers. Provides centralized exception handling
 * across all @RestController annotated classes.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Handles validation errors thrown when method arguments fail validation.
	 *
	 * @param ex the exception containing validation errors
	 * @param request the HTTP request that caused the error
	 * @return a structured response containing all validation error details
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ValidationErrorResponse> handleValidationException(MethodArgumentNotValidException ex,
			HttpServletRequest request) {

		List<ValidationError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(this::mapFieldError)
			.toList();

		ValidationErrorResponse response = new ValidationErrorResponse(LocalDateTime.now(), HttpStatus.BAD_REQUEST.value(),
				HttpStatus.BAD_REQUEST.getReasonPhrase(), "Validation failed for one or more fields", request.getRequestURI(),
				errors);

		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
	}

	private ValidationError mapFieldError(FieldError fieldError) {
		return new ValidationError(fieldError.getField(), fieldError.getRejectedValue(),
				fieldError.getDefaultMessage());
	}

}
