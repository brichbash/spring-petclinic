package org.springframework.samples.petclinic.system;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response object for validation errors, providing structured information about validation
 * failures.
 *
 * @param timestamp the time when the error occurred
 * @param status the HTTP status code
 * @param error the HTTP status reason phrase (e.g., "Bad Request")
 * @param message a general message about the error
 * @param path the request path that caused the error
 * @param errors list of individual field validation errors
 */
public record ValidationErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path,
		List<ValidationError> errors) {
}
