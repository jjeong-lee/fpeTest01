package kr.ac.knue.achievement.common;

import java.util.List;

public class CommandValidationException extends RuntimeException {
    private final List<ApiError.FieldError> fieldErrors;

    public CommandValidationException(ApiError.FieldError fieldError) {
        this(List.of(fieldError));
    }

    public CommandValidationException(List<ApiError.FieldError> fieldErrors) {
        this.fieldErrors = List.copyOf(fieldErrors);
    }

    public List<ApiError.FieldError> fieldErrors() {
        return fieldErrors;
    }
}
