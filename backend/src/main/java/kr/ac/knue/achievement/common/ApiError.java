package kr.ac.knue.achievement.common;

import java.util.List;
import java.util.Map;

public record ApiError(boolean success, ErrorDetail error, Map<String, Object> meta) {
    public static ApiError of(String code, String message) {
        return new ApiError(false, new ErrorDetail(code, message, List.of()), Map.of());
    }

    public static ApiError validation(List<FieldError> fieldErrors) {
        return new ApiError(false, new ErrorDetail("VALIDATION_ERROR", "입력값을 확인해 주세요.", fieldErrors), Map.of());
    }

    public record ErrorDetail(String code, String message, List<FieldError> fieldErrors) { }
    public record FieldError(String field, String message) { }
}
