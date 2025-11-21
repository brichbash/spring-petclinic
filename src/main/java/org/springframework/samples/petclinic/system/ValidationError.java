package org.springframework.samples.petclinic.system;

/**
 * Represents a single field-level validation error.
 *
 * @param field the name of the field that failed validation
 * @param rejectedValue the value that was rejected (may be null)
 * @param message the error message describing why validation failed
 */
public record ValidationError(String field, Object rejectedValue, String message) {
}
