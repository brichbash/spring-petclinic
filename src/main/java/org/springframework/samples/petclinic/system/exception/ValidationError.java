package org.springframework.samples.petclinic.system.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents a single validation error for a specific field or object.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError {

    private String object;
    private String field;
    private Object rejectedValue;
    private String message;

    public ValidationError() {
    }

    public ValidationError(String object, String field, Object rejectedValue, String message) {
        this.object = object;
        this.field = field;
        this.rejectedValue = rejectedValue;
        this.message = message;
    }

    public String getObject() {
        return object;
    }

    public void setObject(String object) {
        this.object = object;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Object getRejectedValue() {
        return rejectedValue;
    }

    public void setRejectedValue(Object rejectedValue) {
        this.rejectedValue = rejectedValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
