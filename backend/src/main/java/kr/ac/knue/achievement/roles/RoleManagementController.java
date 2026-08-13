package kr.ac.knue.achievement.roles;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import kr.ac.knue.achievement.auth.AuthenticationPort;
import kr.ac.knue.achievement.common.ApiError.FieldError;
import kr.ac.knue.achievement.common.ApiException;
import kr.ac.knue.achievement.common.ApiResponse;
import kr.ac.knue.achievement.common.CommandValidationException;
import kr.ac.knue.achievement.common.CommandValidator;

@RestController
public class RoleManagementController {
    private final RoleManagementService service;
    private final AuthenticationPort authenticationPort;
    private final CommandValidator commandValidator;

    public RoleManagementController(RoleManagementService service, AuthenticationPort authenticationPort,
            CommandValidator commandValidator) {
        this.service = service;
        this.authenticationPort = authenticationPort;
        this.commandValidator = commandValidator;
    }

    @GetMapping("/api/roles")
    public ApiResponse<RoleManagementService.RoleSearchResult> search(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0 || size < 1) throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "페이지 정보를 확인해 주세요.");
        return ApiResponse.success(service.search(page, Math.min(size, 100)));
    }

    @PutMapping("/api/roles/{roleCode}")
    public ApiResponse<RoleView> update(@PathVariable String roleCode, @RequestBody Map<String, Object> command,
            HttpServletRequest request) {
        commandValidator.rejectUnexpectedFields(command,
                Set.of("roleName", "grantCriteria", "dataScopeDefault", "useStatus", "reason"));
        commandValidator.requireReason(command, "reason");
        return ApiResponse.success(service.update(roleCode, new RoleUpdateRequest(optionalString(command, "roleName"),
                optionalString(command, "grantCriteria"), optionalString(command, "dataScopeDefault"),
                optionalString(command, "useStatus"), (String) command.get("reason")), actorId(request)));
    }

    private String optionalString(Map<String, Object> command, String field) {
        Object value = command.get(field);
        if (value == null) return null;
        if (!(value instanceof String text)) throw new CommandValidationException(new FieldError(field, "문자열 형식이 올바르지 않습니다."));
        return text;
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