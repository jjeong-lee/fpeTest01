package kr.ac.knue.achievement.users;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.ApiResponse;
import kr.ac.knue.achievement.common.CommandValidator;

@RestController
public class UserManagementController {
    private final UserManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public UserManagementController(UserManagementService service, AuthenticationPort authenticationPort, CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/users")
    public ApiResponse<UserSearchResult> search(
            @RequestParam(required = false) String employeeNo, @RequestParam(required = false) String name,
            @RequestParam(required = false) String organizationCode, @RequestParam(required = false) String position,
            @RequestParam(required = false) String employmentStatus, @RequestParam(required = false) String roleCode,
            @RequestParam(required = false) Boolean systemEnabled, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "페이지 정보를 확인해 주세요.");
        return ApiResponse.success(service.search(new UserSearchCriteria(employeeNo, name, organizationCode, position,
                employmentStatus, roleCode, systemEnabled, page, Math.min(size, 100))));
    }

    @PatchMapping("/api/users/{userId}/system-settings")
    public ApiResponse<UserView> updateSystemSettings(@PathVariable UUID userId, @RequestBody Map<String, Object> command,
            HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command, Set.of("systemEnabled", "roleCodes", "reason"));
        if (!(command.get("systemEnabled") instanceof Boolean systemEnabled)) {
            throw new kr.ac.knue.achievement.common.CommandValidationException(
                    new kr.ac.knue.achievement.common.ApiError.FieldError("systemEnabled", "시스템 사용 여부는 필수입니다."));
        }
        commandValidator.requireReason(command, "reason");
        List<String> roleCodes = command.get("roleCodes") instanceof List<?> values
                ? values.stream().filter(String.class::isInstance).map(String.class::cast).toList() : null;
        return ApiResponse.success(service.updateSystemSettings(userId,
                new UserSystemSettingsRequest(systemEnabled, roleCodes, (String) command.get("reason")), actorId(request)));
    }

    private UUID actorId(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) for (Cookie cookie : cookies) if ("SESSION".equals(cookie.getName())) {
            AuthenticationPort.AuthenticatedSession session = authenticationPort.findActiveSession(cookie.getValue());
            if (session != null) return session.userId();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "인증이 필요합니다.");
    }
}
