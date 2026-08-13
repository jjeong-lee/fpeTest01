package kr.ac.knue.achievement.common;

import java.util.Map;

public record ApiResponse<T>(boolean success, T data, Map<String, Object> meta) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, Map.of());
    }
}
