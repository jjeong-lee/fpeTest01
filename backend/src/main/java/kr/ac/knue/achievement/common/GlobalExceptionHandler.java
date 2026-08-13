package kr.ac.knue.achievement.common;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<Void> handleMissingResource(NoResourceFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<ApiError.FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new ApiError.FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());
        return ResponseEntity.badRequest().body(ApiError.validation(errors));
    }

    @ExceptionHandler(CommandValidationException.class)
    ResponseEntity<ApiError> handleCommandValidation(CommandValidationException exception) {
        return ResponseEntity.badRequest().body(ApiError.validation(exception.fieldErrors()));
    }

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException exception) {
        return ResponseEntity.status(exception.status()).body(ApiError.of(exception.code(), exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        return ResponseEntity.internalServerError().body(ApiError.of("INTERNAL_ERROR", "요청을 처리할 수 없습니다."));
    }
}
