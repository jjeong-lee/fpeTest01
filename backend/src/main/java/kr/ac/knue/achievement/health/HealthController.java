package kr.ac.knue.achievement.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RestController;

import kr.ac.knue.achievement.common.ApiResponse;

@RestController
public class HealthController {
    @GetMapping("/api/health")
    public ApiResponse<Map<String, String>> health() {
        return ApiResponse.success(Map.of("status", "UP"));
    }
}
